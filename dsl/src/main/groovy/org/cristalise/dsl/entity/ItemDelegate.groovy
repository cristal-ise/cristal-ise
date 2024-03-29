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
import org.cristalise.dsl.collection.DependencyDelegate
import org.cristalise.dsl.lifecycle.instance.CompActDelegate
import org.cristalise.dsl.lifecycle.instance.WorkflowBuilder
import org.cristalise.kernel.collection.BuiltInCollections
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.collection.DependencyDescription
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.entity.DomainContext
import org.cristalise.kernel.entity.imports.ImportDependency
import org.cristalise.kernel.entity.imports.ImportDependencyMember
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportOutcome
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.property.PropertyDescriptionList

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 *
 */
@CompileStatic @Slf4j
class ItemDelegate extends PropertyDelegate {

    static String ENTITY_PATTERN = '/entity/'
    public ImportItem newItem = new ImportItem()

    public ItemDelegate(Map<String, Object> args) {
        assert args && args.name && args.folder

        log.debug 'constructor() - args:{}', args

        newItem.namespace = args.ns
        newItem.name      = args.name
        if (args.version != null) newItem.version = (Integer)args.version

        initNewItemFolder(args)
        initNewItemWorkflow(args)

        if (args.workflowVer != null) newItem.workflowVer = (Integer)args.workflowVer
    }

    private void initNewItemFolder(Map<String, Object> args) {
        if (args.folder instanceof DomainContext) {
            newItem.initialPath = ((DomainContext)args.folder).getDomainPath()
        }
        else {
            newItem.initialPath = args.folder
        }
    }

    private void initNewItemWorkflow(Map<String, Object> args) {
        if (args.workflow == null) {
            log.debug 'initNewItemWorkflow() - item:{} will be created without workflow', args.name
        }
        else if (args.workflow instanceof String) {
            newItem.workflow = (String)args.workflow
        }
        else if (args.workflow instanceof CompositeActivityDef) {
            newItem.compActDef = (CompositeActivityDef)args.workflow
            newItem.workflow = newItem.compActDef.name
            if (newItem.compActDef.version != null) {
                newItem.workflowVer = newItem.compActDef.version
                if (args.workflowVer != null) assert newItem.workflowVer == (Integer)args.workflowVer
            }
        }
        else if (args.workflow instanceof Workflow) {
            newItem.wf = (Workflow)args.workflow
        }
        else {
            log.warn 'initNewItemWorkflow() - UNKNOWN class:{} item:{} will be created without workflow', args.workflow.class.getSimpleName(), args.name
        }
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

        if (itemProps) newItem.properties = itemProps
    }

    def Workflow(@DelegatesTo(CompActDelegate) Closure cl) {
        newItem.wf = new WorkflowBuilder().build(cl)
    }

    public void Outcome(Map attr) {
        assert attr
        assert attr.schema
        assert attr.version
        assert attr.viewname
        assert attr.path

        newItem.outcomes.add(new ImportOutcome((String) attr.schema, attr.version as Integer, (String) attr.viewname, (String) attr.path))
    }

    public void Outcome(PropertyDescriptionList propDesc) {
        String schema = 'PropertyDescription'
        Integer version = propDesc.version
        String view = 'last'
        String path = "boot/property/${propDesc.name}_${version}.xml"

        newItem.outcomes.add(new ImportOutcome(schema, version, view, path))
    }

    public void DependencyDescription(BuiltInCollections coll, @DelegatesTo(DependencyDelegate) Closure cl) {
        DependencyDescription(coll.getName(), cl)
    }

    public void DependencyDescription(String name,  @DelegatesTo(DependencyDelegate) Closure cl) {
        Dependency(newItem.namespace, name, true, cl)
    }

    public void Dependency(BuiltInCollections coll, boolean isDescription = false, String classProps = null, @DelegatesTo(DependencyDelegate) Closure cl) {
        Dependency(newItem.namespace, coll.getName(), isDescription, classProps, cl)
    }

    public void Dependency(String name, boolean isDescription = false, String classProps = null, @DelegatesTo(DependencyDelegate) Closure cl) {
        Dependency(newItem.namespace, name, isDescription, classProps, cl)
    }

    public void Dependency(String ns, String name, boolean isDescription = false, String classProps = null, @DelegatesTo(DependencyDelegate) Closure cl) {
        assert name
        assert cl

        def builder = DependencyBuilder.build(ns, name, isDescription, classProps, cl)
        Dependency dependency = builder.dependency

        assert dependency

        ImportDependency idep = new ImportDependency(dependency.name)
        idep.isDescription = dependency instanceof DependencyDescription
        idep.classProps = dependency.classProps

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
