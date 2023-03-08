/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW
import org.cristalise.kernel.collection.BuiltInCollections
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.AddC2KObject
import org.cristalise.kernel.utils.LocalObjectLoader
import groovy.transform.CompileStatic
 

// Fetch the requested name and version from the outcome fields
def wfDefName    = job.getOutcome().getField("WorkflowDefinitionName");
def wfDefVersion = Integer.parseInt(job.getOutcome().getField("WorkflowDefinitionVersion"));

updateWorkflowCollection(item, agent, wfDefName, wfDefVersion)

@CompileStatic
void updateWorkflowCollection(ItemProxy item, AgentProxy agent, String wfDefName, Integer wfDefVersion) {

    // Look up the description
    def  caDef = LocalObjectLoader.getCompActDef(wfDefName, wfDefVersion, item.transactionKey) ;

    // Fetch the 'Workflow' collection
    def coll = (Dependency) item.getCollection(WORKFLOW);

    if (coll.size() > 0) { // if there's already a member, remove it
        def member = coll.getMembers().list.get(0);
        coll.removeMember(member.getID());
    }

    // add the new member
    def member = coll.addMember(caDef.getItemPath(), item.transactionKey);
    member.getProperties().put("Version", wfDefVersion);

    // save it back to the item
    def params = new String[1];
    params[0] = agent.marshall(coll);

    agent.execute(item, AddC2KObject, params);
}
