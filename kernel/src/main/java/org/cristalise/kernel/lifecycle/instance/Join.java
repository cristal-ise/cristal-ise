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

import java.util.Vector;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.graph.traversal.GraphTraversal;
import org.cristalise.kernel.persistency.TransactionKey;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Join extends WfVertex {
    public Vector<String> mErrors;

    public Join() {
        super();
        mErrors = new Vector<String>(0, 1);
    }

    private boolean loopTested;
    public int      counter = 0;

    /**
     * 
     * @return boolean
     * @throws InvalidDataException
     */
    private boolean hasPrevActiveActs() throws InvalidDataException {
        String pairingID = getPairingId();
        boolean findPair = false;

        if (StringUtils.isNotBlank(pairingID)) {
            findPair = true;

            for (Vertex outVertex: getOutGraphables()) {
                String otherPairingID = ((GraphableVertex)outVertex).getPairingId();

                // the pairingID was added to pair with the loop/split (see issue #251), so do not it use for this calculation
                if (pairingID.equals(otherPairingID) && outVertex instanceof Loop) {
                    findPair = false;
                    break;
                }
            }
        }

        Vertex[] vertices;

        if (findPair) {
            GraphableVertex endVertex = findPair();

            if (endVertex == null) throw new InvalidDataException("Could not find pair for Join using PairingID:"+pairingID);

            vertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, endVertex, GraphTraversal.kUp, true);

            log.trace("hasPrevActiveActs(id:{}) - vertices[PairingID:"+pairingID+"]={}", getID(), (Object)vertices);
        }
        else {
            vertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, true);

            log.trace("hasPrevActiveActs(id:{}) - vertices={}", getID(), (Object)vertices);
        }

        for (Vertex v : vertices) {
            if(v instanceof Activity) {
                Activity act = (Activity)v;

                if (!act.isFinished() && act.active) return true;
            }
        }

        return false;
    }

    @Override
    public void runNext(TransactionKey transactionKey) throws InvalidDataException {
        if (!hasPrevActiveActs()) {
            Vertex[] outVertices = getOutGraphables();

            if (ArrayUtils.isNotEmpty(outVertices)) ((WfVertex) outVertices[0]).run(transactionKey);
        }
    }

    public Next addNext(String idNext) {
        return addNext((WfVertex) getParent().search(idNext));
    }

    @Override
    public Next addNext(WfVertex vertex) {
        return new Next(this, vertex);
    }

    @Override
    public void reinit(int idLoop) throws InvalidDataException {
        log.trace("reinit(id:{}, idLoop:{}) - parent:{}", getID(), idLoop, getParent().getName());

        Vertex[] outVertices = getOutGraphables();
        if (outVertices.length == 1) {
            WfVertex nextAct = (WfVertex) outVertices[0];
            nextAct.reinit(idLoop);
        }
    }

    @Override
    public boolean verify() {
        mErrors.removeAllElements();
        int nbOutEdges = getOutEdges().length;
        int nbInEdges = getInEdges().length;
        String type = (String) getProperties().get("Type");
        if (nbInEdges < 1) {
            mErrors.add("not enough previous");
            return false;
        }
        if (type != null && type.equals("Route")) {
            if (nbInEdges > 1) {
                mErrors.add("Bad nb of previous");
                return false;
            }
        }
        if (nbOutEdges > 1) {
            mErrors.add("too many next");
            return false;
        }
        if (nbOutEdges == 0) {
            if (!((CompositeActivity) getParent()).hasGoodNumberOfActivity()) {
                mErrors.add("too many endpoints");
                return false;
            }
        }
        Vertex[] outV = getOutGraphables();
        Vertex[] anteVertices = GraphTraversal.getTraversal(getParent().getChildrenGraphModel(), this, GraphTraversal.kUp, false);
        boolean loop = false;
        boolean errInLoop = false;
        for (int i = 0; i < outV.length; i++) {
            for (int j = 0; j < anteVertices.length; j++)
                if (!loop && outV[i].getID() == anteVertices[j].getID()) {
                    if (outV[i] instanceof Loop) {
                        loop = true;
                        j = anteVertices.length;
                        i = outV.length;
                    }
                    else {
                        errInLoop = true;
                    }
                }
        }
        if (errInLoop && loop) {
            mErrors.add("Problem in Loop");
            return false;
        }
        return true;
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#getErrors()
     */
    @Override
    public String getErrors() {
        if (mErrors.size() == 0)
            return "No error";
        else
            return mErrors.elementAt(0);
    }

    @Override
    public void run(TransactionKey transactionKey) throws InvalidDataException {
        runNext(transactionKey);
    }

    /**
     * @see org.cristalise.kernel.lifecycle.instance.WfVertex#loop()
     */
    @Override
    public boolean loop() {
        boolean loop2 = false;
        if (!loopTested) {
            loopTested = true;
            if (getOutGraphables().length != 0)
                loop2 = ((WfVertex) getOutGraphables()[0]).loop();
        }
        else
            loop2 = true;
        loopTested = false;
        return loop2;
    }

    @Override
    public void runFirst(TransactionKey transactionKey) throws InvalidDataException {
        runNext(transactionKey);
    }

    @Override
    public boolean isJoin() {
        return true;
    }
}
