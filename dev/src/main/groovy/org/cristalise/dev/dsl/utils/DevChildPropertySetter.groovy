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

import org.codehaus.groovy.runtime.InvokerHelper

import groovy.util.ObjectGraphBuilder.ChildPropertySetter
import groovy.util.logging.Slf4j

@Slf4j
class DevChildPropertySetter implements ChildPropertySetter {

    @Override
    public void setChild(Object parent, Object child, String parentName, String propertyName) {
        log.debug 'setChild({}, {})', parentName, propertyName

        Object property = InvokerHelper.getProperty(parent, propertyName);

        if (property != null) {
            if (Collection.class.isAssignableFrom(property.getClass())) {
                ((Collection) property).add(child);
            }
            else if (Map.class.isAssignableFrom(property.getClass())) {
                ((Map) property).put(child.name, child);

                parent.orderOfElements.add(child.name)
            }
            else  {
                InvokerHelper.setProperty(parent, propertyName, child);
            }

            if (parentName == 'Item' && propertyName == 'dependencies') {
                child.from = parent.name
            }
        }
        else {
            InvokerHelper.setProperty(parent, propertyName, child);
        }
    }
}
