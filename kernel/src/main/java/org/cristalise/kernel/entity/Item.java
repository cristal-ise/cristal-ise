package org.cristalise.kernel.entity;

import java.util.List;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@ProxyGen // Generate the proxy and handler
@VertxGen // Generate clients in non-java languages
public interface Item {
    
    public static final String ebAddress = "cristalise-items";

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
            Handler<AsyncResult<String>> returnHandler);

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
    public void queryLifeCycle(String itemUuid, String agentUuid, boolean filter, Handler<AsyncResult<String>> returnHandler);
}
