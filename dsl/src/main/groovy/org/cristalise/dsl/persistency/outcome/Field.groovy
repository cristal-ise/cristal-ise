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
package org.cristalise.dsl.persistency.outcome

import org.cristalise.kernel.common.InvalidDataException


class Field {
    String name
    String type = 'xs:string'
    String pattern
    
    String multiplicity = ''

    String minOccurs = '1'
    String maxOccurs = '1'

    List values
    def defaultVal

    Unit unit


    /**
     * accepted values from XSD specification without namespace (i.e. xs:)
     */
    private static final List types = ['string', 'boolean', 'integer', 'decimal', 'date', 'time', 'dateTime']

    /**
     * Checks if the type is acceptable
     * 
     * @param t
     * @return
     */
    def setType(String t) {
        if( types.contains(t) ) {
            type = "xs:$t"
        }
        else throw new InvalidDataException("Field type '$t' is wrong, it must be one of these: $types")
    }

    private String getMultiplicityVal(String m) {
        def dec = /^\d+$/

        switch(m) {
            case "*"     : return ''
            case ~/$dec/ : return m
            default      : throw new InvalidDataException("Invalid value for multiplicity : '$m'")
        }
    }

    /**
     * 
     * @param m
     * @return
     */
    def setMultiplicity(String m) {
        if(!m) {
            minOccurs = ''; maxOccurs = '';
        }
        else if(m.contains("..")) {
            def vals = m.split(/\.\./)

            def v = getMultiplicityVal(vals[0])

            if(v) minOccurs = v
            else  throw new InvalidDataException("Invalid value for multiplicity : '$m'")

            v = getMultiplicityVal(vals[1])

            if(v) maxOccurs = v
            else  maxOccurs = ''
        }
        else {
            def v = getMultiplicityVal(m)

            if(!v) { minOccurs = '0'; maxOccurs = ''; }
            else   { minOccurs = v;   maxOccurs = v; }
        }

        multiplicity = m
    }

    /**
     * 
     * @param vals
     * @return
     */
    def setValues(List vals) {
        if(unit) throw new InvalidDataException("UNIMPLEMENTED: Cannot use unit and values together")

        values = vals
    }

    /**
     * 'default' is a keyword, so it cannot be used as a variable name, 
     * but this method makes the default keyword usable in the SchemaBuilder DSL
     * 
     * @param val
     * @return
     */
    def setDefault(val) {
        if(values && !values.contains(val)) throw new InvalidDataException("Default value '$val' is wrong, it must be one of these: $values")

        defaultVal = val
    }

    def setUnit(Unit u) {
        if(values) throw new InvalidDataException("UNIMPLEMENTED: Cannot use unit and values together")

        unit = u
    }
}
