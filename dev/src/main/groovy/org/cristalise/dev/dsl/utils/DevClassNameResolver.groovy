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

import groovy.transform.CompileStatic
import groovy.util.ObjectGraphBuilder.ClassNameResolver
import groovy.util.logging.Slf4j


/**
 * Resolves the Class from name used by ObjectGraphBuilder
 */
@Slf4j @CompileStatic
class DevClassNameResolver implements ClassNameResolver {

    @Override
    public String resolveClassname(String className) {
        if ('Item' == className) {
            return 'org.cristalise.dev.dsl.item.CRUDItem'
        }
        else if ('Agent' == className) {
            return 'org.cristalise.dev.dsl.item.CRUDAgent'
        }
        else if ('devDependency' == className || 'dependency' == className) {
            return 'org.cristalise.dev.dsl.item.CRUDDependency'
        }
        else if ('Module' == className) {
            return 'org.cristalise.dev.dsl.module.CRUDModule'
        }
        else {
            return 'org.cristalise.dsl.persistency.outcome.' + className.substring(0, 1).toUpperCase() + className.substring(1)
        }
    }
}
