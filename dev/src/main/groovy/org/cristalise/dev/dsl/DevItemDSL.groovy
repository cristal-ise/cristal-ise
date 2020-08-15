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

package org.cristalise.dev.dsl

import org.cristalise.dsl.entity.AgentBuilder
import org.cristalise.dsl.entity.ItemBuilder
import org.cristalise.dsl.entity.RoleBuilder
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder
import org.cristalise.dsl.lifecycle.definition.ElemActDefBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.querying.QueryBuilder
import org.cristalise.dsl.scripting.ScriptBuilder
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportAgent
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportItem
import org.cristalise.kernel.lifecycle.instance.predefined.ImportImportRole
import org.cristalise.kernel.lifecycle.instance.predefined.ReplaceDomainWorkflow
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.scripting.Script

import groovy.transform.CompileStatic


/**
 *
 */
@CompileStatic
class DevItemDSL extends DevItemUtility {
    public List<ImportRole> Roles(Closure cl) {
        def newRoles = RoleBuilder.build(cl)
        
        newRoles.each { role ->
            agent.execute(agent.getItem('/servers/localhost'), ImportImportRole.class, agent.marshall(role))
        }

        return newRoles
    }

    // name parameter is not used, method is only kept for backward compatibility
    public List<ImportRole> Role(String name, Closure cl) {
        return Roles(cl)
    }

    public ImportAgent Agent(String name, Closure cl) {
        def newAgent = AgentBuilder.build(name, "pwd", cl)
        agent.execute(agent.getItem('/servers/localhost'), ImportImportAgent.class, agent.marshall(newAgent))
        return newAgent
    }

    public ImportItem Item(Map<String, Object> attrs, Closure cl) {
        def newItem = ItemBuilder.build(attrs, cl)
        agent.execute(agent.getItem('/servers/localhost'), ImportImportItem.class, agent.marshall(newItem))

        assert newItem.wf
        newItem.wf.initialise(newItem.itemPath, agent.getPath(), null)

        def newItemProxy = agent.getItem("${attrs.folder}/${attrs.name}")
        agent.execute(newItemProxy, ReplaceDomainWorkflow.class, agent.marshall(newItem.wf.search("workflow/domain")));

        return newItem
    }

    public Schema Schema(String name, String folder, Closure cl) {
        createNewSchema(name, folder)
        def schema = SchemaBuilder.build(name, 0, cl)
        editSchema(name, folder, schema.XSD)
        return schema
    }

    public Query Query(String name, String folder, Closure cl) {
        createNewQuery(name, folder)
        def query = QueryBuilder.build("", name, 0, cl)
        editQuery(name, folder, query.queryXML)
        return query
    }

    public Script Script(String name, String folder, Closure cl) {
        createNewScript(name, folder)
        def script = ScriptBuilder.build("", name, 0, cl)
        editScript(name, folder, script.scriptXML)
        return script.script
    }

    public ActivityDef ElementaryActivityDef(String actName, String folder, Closure cl) {
        createNewElemActDesc(actName, folder)
        def eaDef = ElemActDefBuilder.build(name: (Object)actName, version: 0, cl)
        editElemActDesc(actName, folder, eaDef)
        return eaDef
    }

    public CompositeActivityDef CompositeActivityDef(String actName, String folder, Closure cl) {
        createNewCompActDesc(actName, folder)
        def caDef = CompActDefBuilder.build(name: (Object)actName, version: 0, cl)
        editCompActDesc(actName, folder, caDef)
        return caDef
    }

    public ItemProxy DescriptionItem(String itemName, String folder, Closure cl) {
        def descItem = createNewDescriptionItem(itemName, folder)
        def difd = new DescriptionItemFactoryDelegate()
        difd.processClosure(cl)
        editDescriptionItem(descItem, difd.propDescList, difd.chooseWorkflowXML)
        return descItem
    }
}
