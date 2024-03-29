/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.lifecycle.instance;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.persistency.TransactionKey;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Loop extends XOrSplit {

    public Loop() {
        super();
    }

    @Override
    public boolean loop() {
        return true;
    }

    @Override
    public void followNext(Next activeNext, TransactionKey transactionKey) throws InvalidDataException {
        WfVertex v = activeNext.getTerminusVertex();

        Boolean isMyPair = isMyPair(v);

        if (isMyPair != null) {
            //loop shall trigger reinit for its 'body' only (see issue #251)
            if (isMyPair) v.reinit(getID());
        }
        else {
            //Backward compatibility for workflows without paring id (see issue #251)
            if (isInPrev(v)) v.reinit(getID());
        }

        v.run(transactionKey);
    }

    @Override
    public void reinit(int idLoop) throws InvalidDataException {
        //propagate if the reinit was NOT started by this loop 
        if (idLoop != getID()) {
            log.trace("reinit(id:{}, idLoop:{}) - parent:{}", getID(), idLoop, getParent().getName());

            for (Vertex outVertex: getOutGraphables()) {
                WfVertex v = (WfVertex)outVertex;
                Boolean isMyPair = isMyPair(v);

                if (isMyPair != null) {
                    //loop shall propagate reinit outside of its 'body' (see issue #251)
                    if (!isMyPair) v.reinit(idLoop);
                }
                else {
                    //Backward compatibility for workflows without paring id (see issue #251)
                    if (!isInPrev(v)) v.reinit(idLoop);
                }
            }
        }
        else {
            log.trace("reinit(id:{}, idLoop:{}) - STOPPED!", getID(), idLoop);
        }
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#verify()
     */
    @Override
    public boolean verify() {
        boolean err = super.verify();
        Vertex[] nexts = getOutGraphables();
        Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
        int k = 0;
        int l = 0;
        Vertex[] brothers = getParent().getChildren();
        for (Vertex brother : brothers)
            if (brother instanceof Loop)
                l++;
        for (Vertex next : nexts) {
            for (Vertex anteVertice : anteVertices)
                if (next.getID() == anteVertice.getID())
                    k++;
        }
        if (k != 1 && !(l > 1)) {
            mErrors.add("bad number of pointing back nexts");
            return false;
        }
        // if (nexts.length>2) {
        // mErrors.add("you must only have 2 nexts");
        // return false;
        // }
        return err;
    }

    private boolean isInPrev(Vertex vertex) {
        int id = vertex.getID();
        Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
        for (Vertex anteVertice : anteVertices) {
            if (anteVertice.getID() == id) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isLoop() {
        return true;
    }
}
