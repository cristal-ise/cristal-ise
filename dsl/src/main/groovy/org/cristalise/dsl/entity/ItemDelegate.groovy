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
package org.cristalise.dsl.entity

import org.apache.commons.lang3.StringUtils
import org.cristalise.dsl.collection.DependencyBuilder
import org.cristalise.dsl.lifecycle.instance.WorkflowBuilder
import org.cristalise.kernel.collection.BuiltInCollections
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.collection.DependencyDescription
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.entity.imports.ImportDependency
import org.cristalise.kernel.entity.imports.ImportDependencyMember
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportOutcome
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.process.resource.BuiltInResources

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 *
 */
@CompileStatic @Slf4j
class ItemDelegate extends PropertyDelegate {

    static String ENTITY_PATTERN = '/entity/'
    public ImportItem newItem = new ImportItem()
    List<ImportOutcome> outcomes = new ArrayList<>()

    public ItemDelegate(Map<String, Object> args) {
        assert args && args.name && args.folder

        log.debug 'constructor() - args:{}', args

        newItem.name        = args.name
        newItem.initialPath = args.folder

        if (args.version != null) newItem.version = (Integer)args.version

        if (args.workflow == null) {
            log.debug 'constructor() - item:{} will be created without workflow', args.name
        }
        else if (args.workflow instanceof String) {
            newItem.workflow = (String)args.workflow
        }
        else if (args.workflow instanceof CompositeActivityDef) {
            newItem.compActDef = (CompositeActivityDef)args.workflow
            newItem.workflow = newItem.compActDef.name
            if (newItem.compActDef.version != null) newItem.workflowVer = newItem.compActDef.version
        }
        else if (args.workflow instanceof Workflow) {
            newItem.wf = (Workflow)args.workflow
        }
        else {
            log.warn 'constructor() - UNKNOWN class:{} item:{} will be created without workflow', args.workflow.class.getSimpleName(), args.name
        }

        if (args.workflowVer != null) newItem.workflowVer =  (Integer)args.workflowVer
    }

    public ItemDelegate(String name, String folder, String workflow, Integer workflowVer = null) {
        this(['name': name, 'folder': folder, 'workflow': workflow, 'workflowVer': workflowVer] as Map<String, Object>)
    }

    public ItemDelegate(String name, String folder, CompositeActivityDef caDef) {
        this(['name': name, 'folder': folder, 'workflow': caDef] as Map<String, Object>)
    }

    public void processClosure(Closure cl) {
        assert cl

        Property(Name: newItem.name)

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        if (itemProps) newItem.properties = itemProps.list

        if (outcomes) newItem.outcomes = ArrayList.cast(outcomes)
    }

    def Workflow(Closure cl) {
        newItem.wf = new WorkflowBuilder().build(cl)
    }

    public void Outcome(Map attr) {
        assert attr
        assert attr.schema
        assert attr.version
        assert attr.viewname
        assert attr.path

        outcomes.add(new ImportOutcome((String) attr.schema, attr.version as Integer, (String) attr.viewname, (String) attr.path))
    }

    public void DependencyDescription(BuiltInCollections coll, Closure cl) {
        DependencyDescription(coll.getName(), cl)
    }

    public void DependencyDescription(String name, Closure cl) {
        Dependency(name, true, cl)
    }

    public void Dependency(BuiltInCollections coll, boolean isDescription = false, Closure cl) {
        Dependency(coll.getName(), isDescription, cl)
    }
   
    public void Dependency(String name, boolean isDescription = false, Closure cl) {
        assert name
        assert cl

        def builder = DependencyBuilder.build(name, isDescription, cl)
        Dependency dependency = builder.dependency

        assert dependency

        ImportDependency idep = new ImportDependency(dependency.name)
        idep.isDescription = dependency instanceof DependencyDescription

        dependency.members.list.each { mem ->
            DependencyMember member = DependencyMember.cast(mem)
            String itemPath = member.itemPath.stringPath

            //
            if (itemPath.startsWith(ENTITY_PATTERN) && !ItemPath.isUUID(itemPath))
                itemPath = itemPath.replaceFirst(ENTITY_PATTERN, StringUtils.EMPTY)

            ImportDependencyMember imem = new ImportDependencyMember(itemPath)
            imem.props = member.properties
            idep.dependencyMemberList << imem
        }
        
        if (dependency.getProperties().size() > 0) {
          idep.props = dependency.getProperties()
        }

        newItem.dependencyList.add(idep)
    }
}
