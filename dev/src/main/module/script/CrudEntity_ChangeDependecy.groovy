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
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*

import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.AddMembersToCollection
import org.cristalise.kernel.lifecycle.instance.predefined.RemoveMembersFromCollection
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.utils.CastorHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic
import groovy.transform.Field

@Field
final Logger log = LoggerFactory.getLogger("org.cristalise.dev.Script.CrudEntity_ChangeDependecy")


/**
 * 
 * @param dependencyName
 * @param outcome
 * @return
 */
@CompileStatic
def Dependency processAddMembersToCollection(AgentProxy theAgent, Job theJob, String dependencyName, Outcome outcome) {
    def memberPath = outcome.getField('MemberPath')
    def memberName = outcome.getField('MemberName')

    if (!memberPath && !memberName) {
        log.error('processAddMembersToCollection() - MemberPath and MemberName is missing from outcome:{}', outcome)
        throw new InvalidDataException('Please provide MemberPath or MemberName')
    }

    def dep = new Dependency(dependencyName)

    if (memberPath) {
        dep.addMember(theAgent.getItem(memberPath).getPath(), new CastorHashMap(), '', null);
    }
    else {
        // find the item in the 'default' location eg. /integTest/Patients/kovax
        def moduleNs = theJob.getActProp('ModuleNameSpace')
        dep.addMember(theAgent.getItem("$moduleNs/$dependencyName/$memberName").getPath(), new CastorHashMap(), '', null);
    }

    return dep
}

/**
 * 
 * @param dependencyName
 * @param outcome
 * @return
 */
@CompileStatic
def Dependency processRemoveMembersFromCollection(ItemProxy theItem, AgentProxy theAgent, Job theJob, String dependencyName, Outcome outcome) {
    def memberSlotId = outcome.hasField('MemberSlotId') ? outcome.getField('MemberSlotId') as Integer : -1
    def dep = new Dependency(dependencyName)

    if (memberSlotId != -1) {
        def currDep = (Dependency)theItem.getCollection(dependencyName)
        def member = currDep.getMember(memberSlotId)

        dep.addMember(member)
    }
    else {
        log.error('processRemoveMembersFromCollection() - MemberSlotId was not set in outcome:{}', outcome)
        throw new InvalidDataException('Please provide MemberSlotId')
    }

    return dep
}

/*
 * Script starts here 
 */
Outcome outcome = job.getOutcome()
def dependencyName = job.getActProp(DEPENDENCY_NAME)
def predefinedStep = job.getActProp(PREDEFINED_STEP)

if (predefinedStep == AddMembersToCollection.class.getSimpleName()) {
    if (outcome.getNodeByXPath('//AddMembersToCollection/Dependency')) {
        log.debug('//AddMembersToCollection/Dependency is already in Outcome - job:{}', job);
    }
    else {
        def dep = processAddMembersToCollection(agent, job, dependencyName, outcome)
        outcome.appendXmlFragment('//AddMembersToCollection', agent.marshall(dep))
    }
}
else if (predefinedStep == RemoveMembersFromCollection.class.getSimpleName()) {
    if (outcome.getNodeByXPath('//RemoveMembersFromCollection/Dependency')) {
        log.debug('//RemoveMembersFromCollection/Dependency is already in Outcome - job:{}', job);
    }
    else {
        def dep = processRemoveMembersFromCollection(item, agent, job, dependencyName, outcome)
        outcome.appendXmlFragment('//RemoveMembersFromCollection', agent.marshall(dep))
    }
}
else {
    throw new InvalidDataException("Script CrudEntity_ChangeDependecy cannot handle predefined step:"+predefinedStep);
}

