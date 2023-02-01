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
package org.cristalise.dev.dsl

import static org.cristalise.kernel.process.resource.BuiltInResources.*
import org.cristalise.dev.scaffold.DevItemCreator
import org.cristalise.dsl.entity.AgentBuilder
import org.cristalise.dsl.entity.AgentDelegate
import org.cristalise.dsl.entity.ItemBuilder
import org.cristalise.dsl.entity.ItemDelegate
import org.cristalise.dsl.entity.RoleBuilder
import org.cristalise.dsl.entity.RoleDelegate
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder
import org.cristalise.dsl.lifecycle.definition.CompActDefDelegate
import org.cristalise.dsl.lifecycle.definition.ElemActDefBuilder
import org.cristalise.dsl.lifecycle.definition.ElemActDefDelegate
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.persistency.outcome.SchemaDelegate
import org.cristalise.dsl.querying.QueryBuilder
import org.cristalise.dsl.querying.QueryDelegate
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.dsl.scripting.ScriptDelegate
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.entity.Job
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportAgent
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportItem
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportRole
import org.cristalise.kernel.lifecycle.instance.predefined.ReplaceDomainWorkflow
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.DefaultResourceImportHandler
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.utils.DescriptionObject
import org.cristalise.kernel.utils.ItemDescCache
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 *
 */
@CompileStatic @Slf4j
class DevItemDSL {
    public AgentProxy agent = null
    public DevItemCreator creator = null

    public Job getDoneJob(ItemProxy proxy, String actName) {
        log.info('getDoneJob() - proxy:{} actName:{}', proxy, actName)
        Job j = proxy.getJobByName(actName, agent)
        assert j && j.getStepName() == actName && j.transition.name == "Done"
        return j
    }

    public Job executeDoneJob(ItemProxy proxy, String actName, String outcomeXML) {
        def job = getDoneJob(proxy, actName)

        if (outcomeXML)            job.outcome = outcomeXML
        else if (job.hasOutcome()) job.outcome = job.getOutcome() //this calls outcome initiator if defined

        String resultOutcome = agent.execute(job)
        if (job.hasOutcome()) assert resultOutcome

        return job
    }

    public Job executeDoneJob(ItemProxy proxy, String actName, Outcome outcome = null) {
        def job = getDoneJob(proxy, actName)

        if (outcome)               job.outcome = outcome
        else if (job.hasOutcome()) job.outcome = job.getOutcome() //this calls outcome initiator if defined

        String resultOutcome = agent.execute(job)
        if (job.hasOutcome()) assert resultOutcome

        return job
    }

    public List<ImportRole> Roles(@DelegatesTo(RoleDelegate) Closure cl) {
        def newRoles = RoleBuilder.build(cl)
        def resultRoles = new ArrayList<ImportRole>()

        newRoles.each { role ->
            def result = agent.execute(agent.getItem('/servers/localhost'), ImportImportRole.class, agent.marshall(role))
            resultRoles.add((ImportRole) Gateway.getMarshaller().unmarshall(result))
        }

        return resultRoles
    }

    // name parameter is not used, method is only kept for backward compatibility
    public List<ImportRole> Role(String name, @DelegatesTo(RoleDelegate) Closure cl) {
        return Roles(cl)
    }

    public ImportAgent Agent(String name, @DelegatesTo(AgentDelegate) Closure cl) {
        def newAgent = AgentBuilder.build(name, "pwd", cl)
        def result = agent.execute(agent.getItem('/servers/localhost'), ImportImportAgent.class, agent.marshall(newAgent))
        return (ImportAgent) Gateway.getMarshaller().unmarshall(result)
    }

    public ImportItem Item(Map<String, Object> attrs, @DelegatesTo(ItemDelegate) Closure cl) {
        def newItem = ItemBuilder.build(attrs, cl)
        def result = agent.execute(agent.getItem('/servers/localhost'), ImportImportItem.class, agent.marshall(newItem))

        assert newItem.wf
        newItem.wf.initialise(newItem.itemPath, agent.getPath(), null)

        def newItemProxy = agent.getItem("${attrs.folder}/${attrs.name}")
        agent.execute(newItemProxy, ReplaceDomainWorkflow.class, agent.marshall(newItem.wf.search("workflow/domain")));

        return (ImportItem) Gateway.getMarshaller().unmarshall(result)
    }

    private ItemProxy createOrUpdate(String namespace, DescriptionObject descObj) {
        if (namespace) descObj.namespace = namespace

        if (descObj.exists(null)) {
            return creator.updateItemAndMoveLatestVersion(descObj)
        }
        else {
            return creator.createItemWithUpdateAndAssignNewVersion(descObj)
        }
    }

    public ImportItem ItemDesc(String name, String folder, @DelegatesTo(ItemDelegate) Closure cl) {
        def itemDesc = ItemBuilder.build(name: name, version: 0, cl)
        createOrUpdate(folder, itemDesc)
        return itemDesc
    }

    public ImportAgent AgentDesc(String name, String folder, @DelegatesTo(AgentDelegate) Closure cl) {
        def agentDesc = AgentBuilder.build(name: name, version: 0, cl)
        createOrUpdate(folder, agentDesc)
        return agentDesc
    }

    public List<ImportRole> RoleDescList(String name, String folder, @DelegatesTo(RoleDelegate) Closure cl) {
        def roleDescList = RoleBuilder.build(cl)
        roleDescList.each { roleDesc ->
            createOrUpdate(folder, roleDesc)
        }
        return roleDescList
    }

    public Schema Schema(String name, String folder, @DelegatesTo(SchemaDelegate) Closure cl) {
        def schema = SchemaBuilder.build(name, 0, cl).schema
        createOrUpdate(folder, schema)
        return schema
    }

    public Query Query(String name, String folder, @DelegatesTo(QueryDelegate) Closure cl) {
        def query = QueryBuilder.build("", name, 0, cl)
        createOrUpdate(folder, query)
        return query
    }

    public Script Script(String name, String folder, @DelegatesTo(ScriptDelegate) Closure cl) {
        Script script = ScriptBuilder.build("", name, 0, cl).script
        createOrUpdate(folder, script)
        return script
    }

    public ActivityDef ElementaryActivityDef(String actName, String folder, @DelegatesTo(ElemActDefDelegate) Closure cl) {
        def eaDef = ElemActDefBuilder.build(name: (Object)actName, version: 0, cl)
        createOrUpdate(folder, eaDef)
        return eaDef
    }

    public CompositeActivityDef CompositeActivityDef(String actName, String folder, @DelegatesTo(CompActDefDelegate) Closure cl) {
        def caDef = CompActDefBuilder.build(name: (Object)actName, version: 0, cl)
        createOrUpdate(folder, caDef)
        return caDef
    }

    public ItemProxy DescriptionItem(String itemName, String folder, @DelegatesTo(DescriptionItemFactoryDelegate) Closure cl) {
        def descItem = creator.createItem(itemName, '/desc/dev/DescriptionFactory')

        def difd = new DescriptionItemFactoryDelegate()
        difd.processClosure(cl)

        executeDoneJob(descItem, "SetPropertyDescription", Gateway.getMarshaller().marshall(difd.propDescList) )
        executeDoneJob(descItem, "SetInstanceWorkflow",    difd.chooseWorkflowXML)

        return descItem
    }
}
