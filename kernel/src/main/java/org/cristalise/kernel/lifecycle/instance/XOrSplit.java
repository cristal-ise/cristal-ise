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

import static org.cristalise.kernel.graph.model.BuiltInEdgeProperties.ALIAS;

import java.util.ArrayList;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.persistency.TransactionKey;

public class XOrSplit extends Split {
    
    public XOrSplit() {
        super();
    }

    @Override
	public void runNext(TransactionKey transactionKey) throws InvalidDataException {
        String[] nextsTab = calculateNexts(transactionKey);

        ArrayList<DirectedEdge> nextsToFollow = new ArrayList<DirectedEdge>();

        for (DirectedEdge outEdge : getOutEdges()) {
            String alias = (String)((Next)outEdge).getBuiltInProperty(ALIAS, "");

            if (isInTable(alias, nextsTab)) nextsToFollow.add(outEdge);
        }

        if (nextsToFollow.size() != 1) {
            throw new InvalidDataException("not good number of active next! (id:"+getID()+")");
        }

        followNext((Next)nextsToFollow.get(0), transactionKey);
    }

    public void followNext(Next activeNext, TransactionKey transactionKey) throws InvalidDataException {
        activeNext.getTerminusVertex().run(transactionKey);
    }
}
