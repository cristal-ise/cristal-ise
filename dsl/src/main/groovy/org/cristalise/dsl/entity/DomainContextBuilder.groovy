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

import org.cristalise.kernel.entity.DomainContext
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import groovy.transform.CompileStatic

@CompileStatic
class DomainContextBuilder {
    public static List<DomainContext> build(Map<String, Object> attrs, @DelegatesTo(DomainContextDelegate) Closure cl) {
        assert attrs, "cannot work with empty attributes (Map)"

        def delegate = new DomainContextDelegate(attrs)
        delegate.processClosure(cl)

        return delegate.newContexts
    }

    public static List<DomainContext> build(String ns, Integer version = 0, @DelegatesTo(DomainContextDelegate) Closure cl) {
        return build(['ns': ns, 'version': version], cl)
    }
}
