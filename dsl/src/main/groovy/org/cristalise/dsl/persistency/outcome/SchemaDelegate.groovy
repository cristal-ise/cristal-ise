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

import groovy.xml.MarkupBuilder

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.utils.Logger


/**
 *
 */
class SchemaDelegate {

    String xsdString

    public void processClosure(Closure cl) {
        assert cl, "Schema only works with a valid Closure"

        Logger.msg 1, "Schema(start) ---------------------------------------"

        def objBuilder = new ObjectGraphBuilder()
        objBuilder.classLoader = this.class.classLoader
        objBuilder.classNameResolver = 'org.cristalise.dsl.persistency.outcome'

        cl.delegate = objBuilder

        xsdString = buildXSD( cl() )

        Logger.msg 1, "Schema(end) +++++++++++++++++++++++++++++++++++++++++"
    }

    public String buildXSD(Struct s) {
        if(!s) throw new InvalidDataException("Schema cannot be built from empty declaration")
        
        def writer = new StringWriter()
        def xsd = new MarkupBuilder(writer)

        xsd.setOmitEmptyAttributes(true)
        xsd.setOmitNullAttributes(true)

        xsd.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")

        xsd.'xs:schema'('xmlns:xs': 'http://www.w3.org/2001/XMLSchema') {
            buildStruct(xsd,s)
        }

        return writer.toString()
    }

    private void buildStruct(xsd, Struct s) {
        Logger.msg 1, "SchemaDelegate.buildStruct() - Struct: $s.name"

        xsd.'xs:element'(name: s.name) {

            if(s.documentation) 'xs:annotation' { 'xs:documentation'(s.documentation) }

            'xs:complexType' {
                'xs:sequence' {
                    s.fields.each { Field f -> buildField(xsd, f) }
                }
            }
        }
    }

    private void buildField(xsd, Field f) {
        Logger.msg 1, "SchemaDelegate.buildField() - Field: $f.name"

        xsd.'xs:element'(name: f.name, type: (!f.values && !f.unit ? f.type : ''), 'default': f.defaultVal, minOccurs: f.minOccurs, maxOccurs: f.maxOccurs) {
            if(f.unit) {
                'xs:complexType' {
                    'xs:simpleContent' {
                        'xs:extension'(base: f.type) {
                            'xs:attribute'(name:"unit", type: (!f.unit.values ? 'xs:string' : ''), 'default': f.unit.defaultVal, 'use': (f.unit.required && f.unit.defaultVal ? "optional": "required")) {
                                if(f.unit.values) {
                                    buildRestriction(xsd, 'xs:string', f.unit.values)
                                }
                            }
                        }
                    }
                }
            }
            else if(f.values) {
                buildRestriction(xsd, f.type, f.values)
            }
        }
    }

    private void buildRestriction(xsd, String type, List values) {
        Logger.msg 1, "SchemaDelegate.buildRestriction() - type:$type, values: $values"

        xsd.'xs:simpleType' {
            'xs:restriction'(base: type) {
                values.each {
                    'xs:enumeration'(value: it)
                }
            }
        }
    }
}
