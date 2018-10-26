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

import groovy.transform.CompileStatic
import org.apache.commons.lang3.StringUtils
import org.cristalise.dsl.collection.DependencyBuilder
import org.cristalise.dsl.lifecycle.instance.WorkflowBuilder
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.entity.imports.ImportDependency
import org.cristalise.kernel.entity.imports.ImportDependencyMember
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportOutcome
import org.cristalise.kernel.lookup.ItemPath

/**
 *
 */
@CompileStatic
class ItemDelegate extends PropertyDelegate {

    public ImportItem newItem = new ImportItem()
    List<ImportOutcome> outcomes = new ArrayList<>()

    public ItemDelegate(String name, String folder, String workflow) {
        newItem.name = name
        newItem.initialPath = folder
        newItem.workflow = workflow
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

    public void Dependency(String name, Closure cl) {
        assert name
        assert cl

        def builder = DependencyBuilder.build(name, cl)
        Dependency dependency = builder.dependency

        assert  dependency

        ImportDependency idep = new ImportDependency(dependency.name)
        idep.version = dependency.version
        idep.isDescription = dependency.properties.get('isDescription')

        dependency.members.list.each { mem ->
            DependencyMember member = DependencyMember.cast(mem)
            ImportDependencyMember imem = new ImportDependencyMember()
            imem.props = member.properties
            imem.itemPath = member.itemPath
            idep.dependencyMemberList << imem
        }

        newItem.dependencyList.add(idep)
    }
}
