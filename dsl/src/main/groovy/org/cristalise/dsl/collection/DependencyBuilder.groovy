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

import groovy.transform.CompileStatic

import org.cristalise.kernel.collection.Dependency


/**
 *
 */
@CompileStatic
class DependencyBuilder {
    Dependency dependency

    public DependencyBuilder(Dependency d) {
        dependency = d
    }

    public static DependencyBuilder build(String name, boolean isDescrption = false, String classProps = null, @DelegatesTo(DependencyDelegate) Closure cl) {
        return build("", name, isDescrption, classProps, cl)
    }

    public static DependencyBuilder build(String ns, String name, boolean isDescrption = false, String classProps = null, @DelegatesTo(DependencyDelegate) Closure cl) {
        def delegate = new DependencyDelegate(ns, name, isDescrption, classProps)

        delegate.processClosure(cl)

        return new DependencyBuilder(delegate.dependency)
    }
}
