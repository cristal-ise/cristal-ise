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
package org.cristalise.dsl.collection

import org.cristalise.dsl.property.PropertyBuilder
import org.cristalise.dsl.property.PropertyDelegate
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.collection.DependencyDescription
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.BuiltInResources
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.utils.DescriptionObject

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * 
 *
 */
@CompileStatic @Slf4j
class DependencyDelegate {
    Dependency dependency
    String moduleNs

    public DependencyDelegate(String name, boolean isDescription) {
        this(null, name, isDescription)
    }

    public DependencyDelegate(String ns, String name, boolean isDescription) {
        this(ns, name, isDescription, null)
    }

    public DependencyDelegate(String ns, String name, boolean isDescription, String classProps) {
        moduleNs = ns
        dependency = isDescription ? new DependencyDescription(name) : new Dependency(name)
        if (classProps) dependency.setClassProps(classProps)
    }

    public void  processClosure(Closure cl) {
        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void Properties(@DelegatesTo(PropertyDelegate) Closure cl) {
        dependency.properties = PropertyBuilder.build(cl)
    }

    public void Member(DescriptionObject descObject, @DelegatesTo(DependencyMemberDelegate) Closure cl = null) {
        Member(moduleNs: moduleNs, itemPath: descObject, cl)
    }

    public void Member(Map attrs, @DelegatesTo(DependencyMemberDelegate) Closure cl = null) {
        assert attrs && attrs.itemPath

        String iPathStr

        if (attrs.itemPath instanceof DescriptionObject) {
            def descObject = (DescriptionObject)attrs.itemPath
            def typeRoot = BuiltInResources.getValue(descObject).getTypeRoot();

            moduleNs = moduleNs ?: (String)attrs.moduleNs

            assert moduleNs, "'moduleNs' variable shall not be blank"

            iPathStr = "$typeRoot/$moduleNs/${descObject.name}"
        }
        else 
            iPathStr = (String)attrs.itemPath

        assert iPathStr, "'itemPath' variable shall not be blank"

        if (checkItemExists(iPathStr))
            log.debug "Unable to resolve the Item for '$iPathStr' - perhaps Item was not created yet"

        //HACK: iPathStr is very likely contains a domainPath. itemPath is created 'manually' because of addMember()
        ItemPath itemPath = new ItemPath()
        itemPath.path[0] = iPathStr
        def member = dependency.addMember(itemPath, null)

        if (cl) {
            DependencyMemberDelegate delegate = new DependencyMemberDelegate()
            delegate.processClosure(cl)
            member.properties << delegate.props
        }
    }

    private boolean checkItemExists(String pathString) {
        Path path

        if (ItemPath.isUUID(pathString)) path = new ItemPath(pathString);
        else                             path = new DomainPath(pathString)
            
        if (Gateway.getLookup()) return Gateway.getLookup().exists(path)
        else                     return false
    }
}
