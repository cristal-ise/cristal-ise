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
package org.cristalise.dev.dsl.utils

import org.atteo.evo.inflector.English
import org.codehaus.groovy.runtime.InvokerHelper

import groovy.util.ObjectGraphBuilder.RelationNameResolver
import groovy.util.logging.Slf4j

@Slf4j
class DevRelationNameResolver implements RelationNameResolver {

    @Override
    public String resolveChildRelationName(String parentName, Object parent, String childName, Object child) {
        log.debug('resolveChildRelationName() - parentName:{} childName:{}', parentName, childName)

        if      (parentName == 'Item'   && childName == 'dependency') return 'dependencies'
        else if (parentName == 'Module' && childName == 'Item')       return 'items'

        //code copied from DefaultRelationNameResolver and optimised to use English.plural()
        String childNamePlural = English.plural(childName)

        MetaProperty metaProperty = InvokerHelper.getMetaClass(parent).hasProperty(parent, childNamePlural);

        return metaProperty != null ? childNamePlural : childName;
    }

    @Override
    public String resolveParentRelationName(String parentName, Object parent, String childName, Object child) {
        log.debug('resolveParentRelationName() - parentName:{} childName:{}', parentName, childName)

        //code copied from DefaultRelationNameResolver
        return parentName
    }
}
