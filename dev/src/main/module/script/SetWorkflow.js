/*
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

// Fetch the requested name from the outcome field
var wfDefName    = job.getOutcome().getField("WorkflowDefinitionName");
var wfDefVersion = job.getOutcome().getField("WorkflowDefinitionVersion");

// Look up the description
var wfItem;

try {
    wfItem = agent.searchItem(new org.cristalise.kernel.lookup.DomainPath("/desc/ActivityDesc"), wfDefName);
}
catch (e) {
    throw wfDefName+" is not a valid item; Exception - " + e; 
}

// Make sure it has the right properties to be a composite activity desc
var itemType = wfItem.getProperty("Type");
var complex  = wfItem.getProperty("Complexity");

if (!itemType.equals("ActivityDesc")) throw wfDefName+" is not an activity description";
if (!complex.equals("Composite"))     throw wfDefName+" is not a composite activity description";

// Check that the named version exists
try {
    wfItem.getObject("/ViewPoint/CompositeActivityDef/"+wfDefVersion);
}
catch (e) {
    throw wfDefName + " does not contain a version "+wfDefVersion + "; Exception - " + e;
}

// Fetch the 'Workflow' collection
var coll = item.getObject("/Collection/workflow/last");

if (coll.size() > 0) { // if there's already a member, remove it
    var member = coll.getMembers().list.get(0);
    coll.removeMember(member.getID());
}

// add the new member
var cm = coll.addMember(wfItem.getPath(), item.getTransactionKey());
cm.getProperties().put("Version", wfDefVersion);

// save it back to the item
var params = new Array(1);
params[0] = agent.marshall(coll);
agent.execute(item, "AddC2KObject", params);
