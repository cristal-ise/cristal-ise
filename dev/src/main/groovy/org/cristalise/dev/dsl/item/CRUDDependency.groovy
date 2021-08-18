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
package org.cristalise.dev.dsl.item

import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*

import org.atteo.evo.inflector.English
import org.cristalise.kernel.collection.Collection.Cardinality
import org.cristalise.kernel.collection.Collection.Type

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor

@CompileStatic @MapConstructor
class CRUDDependency {
    String      name
    String      from 
    String      to
    Type        type
    Cardinality cardinality
    
    List<String> otherDependencyNames = []

    /**
     * Specifies if this Dependency instance originates the relationship which means 
     * the Item containing this Dependency has the workflow to manage the it
     */
    Boolean originator = true

    public void setType(Type t) {
        type = t
    }

    public void setType(String t) {
        type = Type.valueOf(t)
    }

    public void setCardinality(String c) {
        cardinality = Cardinality.valueOf(c)
    }

    public void setCardinality(Cardinality c) {
        cardinality = c
    }
//
    public String getName() {
        if (!name) {
            if (cardinality == ManyToMany || cardinality == OneToMany) {
                name = English.plural(to)
            }
            else {
                name = to
            }
        }
        return name
    }

    public String getOtherNames() {
        if (otherDependencyNames) {
            return otherDependencyNames.join(',')
        }
        else {
            if (cardinality == ManyToMany || cardinality == ManyToOne) {
                return English.plural(from)
            }
            else {
                return from
            }
        }
    }

    public String getPlantUml() {
        String fromMany = cardinality == ManyToMany || cardinality == ManyToOne ? '"*" ' : ''
        String toMany   = cardinality == ManyToMany || cardinality == OneToMany ? ' "*"' : ''
        String toArrow  = type == Unidirectional ? '">"' : ''

        return "${from} ${fromMany}--${toArrow}${toMany} ${to}\n".toString()
    }
}
