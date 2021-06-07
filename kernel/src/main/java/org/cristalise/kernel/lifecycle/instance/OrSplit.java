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
import java.util.Arrays;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrSplit extends Split {

    public OrSplit() {
        super();
    }

    @Override
    public void runNext(AgentPath agent, ItemPath itemPath, TransactionKey transactionKey) throws InvalidDataException {
        int id = getID();
        String[] nextsTab = calculateNexts(itemPath, transactionKey);

        ArrayList<DirectedEdge> nextsToFollow = new ArrayList<DirectedEdge>();

        log.debug("runNext(id:{}) - Finding edge with {} '{}'", id, ALIAS, Arrays.toString(nextsTab));

        for (DirectedEdge outEdge : getOutEdges()) {
            if (isInTable((String)((Next)outEdge).getBuiltInProperty(ALIAS), nextsTab)) {
                nextsToFollow.add(outEdge);
            }
        }

        if (nextsToFollow .size() == 0) {
            throw new InvalidDataException("No edges found, no next vertex activated! (id: " + id + ")");
        }

        for (DirectedEdge edge : nextsToFollow) {
            Next next = (Next)edge;
            log.debug("runNext(id:{}) - Running {}", id, next.getBuiltInProperty(ALIAS));
            next.getTerminusVertex().run(agent, itemPath, transactionKey);
        }
    }

}
