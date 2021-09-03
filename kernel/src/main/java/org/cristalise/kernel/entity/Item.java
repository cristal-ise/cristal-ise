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
package org.cristalise.kernel.entity;

import java.util.List;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

@ProxyGen // Generate the proxy and handler
@VertxGen // Generate clients in non-java languages
public interface Item {
    /**
     * Requests a transition of an Activity in this Item's workflow. If possible and permitted, an Event is generated and stored, the
     * Activity's state is updated, which may cause the Workflow to proceed. If this transition requires Outcome data, this is supplied and
     * stored, and a Viewpoint will be created or updated to point to this latest version. In the case of PredefinedSteps, additional data
     * changes may be performed in the server data.
     * 
     * This method should not be called directly, as there is a large client side to activity execution implemented in the Proxy objects,
     * such as script execution and schema validation.
     *
     * @param itemUuid The UUID of the Item to be requested.
     * @param agentKey The UUID of the Agent. Some activities may be restricted in which roles may execute them. 
     *                 Some transitions cause the activity to be assigned to the executing Agent.
     * @param stepPath The path in the Workflow to the desired Activity
     * @param transitionID The transition to be performed 
     * @param requestData The XML Outcome of the work defined by the Activity. Must be valid to the XML Schema, 
     *                    though this is not verified on the server, rather in the AgentProxy in the Client API.
     * @param fileName the name of the file associated with attachment
     * @param attachment binary data associated with the Outcome (can be empty)
     * @param returnHandler vert.x way to return the potentially updated Outcome or Exception
     **/
    public void requestAction(
            String     itemUuid, 
            String     agentUuid, 
            String     stepPath, 
            int        transitionID, 
            String     requestData, 
            String     fileName, 
            List<Byte> attachment,
            Handler<AsyncResult<JsonObject>> returnHandler);

    /**
     * Returns a set of Jobs for this Agent on this Item. Each Job represents a possible transition of a particular 
     * Activity in the Item's lifecycle. The list may be filtered to only refer to currently active activities.
     *
     * @param itemUuid The UUID of the Item to be queried.
     * @param agentUuid The UUID the Agent requesting Jobs.
     * @param filter If true, then only Activities which are currently active will be included.
     * @param returnHandler vert.x way to return the marshaled {@link org.cristalise.kernel.entity.agent.JobArrayList JobArrayList}
     *                      or Exception
     **/
    public void queryLifeCycle(String itemUuid, String agentUuid, boolean filter, Handler<AsyncResult<JsonObject>> returnHandler);
}
