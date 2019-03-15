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
        xsd.'xs:element'(name: s.name, minOccurs: s.minOccurs, maxOccurs: s.maxOccurs) {

            if(s.documentation) 'xs:annotation' { 'xs:documentation'(s.documentation) }

            'xs:complexType' {
                if(s.fields || s.structs || s.anyField) {
                    if(s.useSequence) {
                        'xs:sequence' {
                            if(s.fields)  s.fields.each  { Field f   -> buildField(xsd, f) }
                            if(s.structs) s.structs.each { Struct s1 -> buildStruct(xsd, s1) }
                            if(s.anyField) buildAnyField(xsd, s.anyField)
                        }
                    }
                    else {
                        'xs:all'(minOccurs: '0') {
                            if(s.fields)  s.fields.each  { Field f   -> buildField(xsd, f) }
                            if(s.structs) s.structs.each { Struct s1 -> buildStruct(xsd, s1) }
                            if(s.anyField) buildAnyField(xsd, s.anyField)
                        }
                    }
                }
                if(s.attributes) {
                    s.attributes.each { Attribute a -> buildAtribute(xsd, a) }
                }
            }
        }
    }

    private boolean hasRangeConstraints(Attribute a) {
        return a.minInclusive != null || a.maxInclusive != null || a.minExclusive != null || a.maxExclusive != null
    }

    private boolean hasRestrictions(Attribute a) {
        return a.values || a.pattern || hasRangeConstraints(a) || a.totalDigits != null || a.fractionDigits != null
    }

    /**
     * Checks whether the field has a restriction/attributes/unit or not, because the type of the element 
     * is either specified in the 'type' attribute or in the restriction as 'base'
     * 
     * @param f the actual field to check
     * @return the type if the field has no restriction, otherwise an empty string
     */
    private String fieldType(Field f) {
        if (hasRestrictions(f) || f.attributes || f.unit) return ''
        else                                              return f.type
    }

    /**
     * Checks whether the attribute has a restriction or not, because the type of the attribute
     * is either specified in the 'type' attribute or in the restriction as 'base'
     * 
     * @param a the attribute to check
     * @return the type if the attribute has no restriction, otherwise an empty string
     */
    private String attributeType(Attribute a) {
        if (hasRestrictions(a)) return ''
        else                    return a.type 
    }
 
    private void buildAtribute(xsd, Attribute a) {
        Logger.msg 1, "SchemaDelegate.buildAtribute() - attribute: $a.name"

        if (a.documentation) throw new InvalidDataException('Atttrbute cannotnot define documentation')

        xsd.'xs:attribute'(name: a.name, type: attributeType(a), 'default': a.defaultVal, 'use': (a?.required ? "required": "")) {
            if(hasRestrictions(a)) {
                buildRestriction(xsd, a.type, a.values, a.pattern, a, a.totalDigits, a.fractionDigits )
            }
        }
    }

    private void setAppinfoDynamicForms(xsd, Field f) {
        xsd.dynamicForms {
            if (f.dynamicForms.hidden   != null) hidden(   f.dynamicForms.hidden)
            if (f.dynamicForms.required != null) required( f.dynamicForms.required)
            if (f.dynamicForms.disabled != null) disabled( f.dynamicForms.disabled)
            if (f.dynamicForms.multiple != null) multiple( f.dynamicForms.multiple)
            if (f.dynamicForms.label)            label(    f.dynamicForms.label)
            if (f.dynamicForms.type)             type(     f.dynamicForms.type)
            if (f.dynamicForms.inputType)        inputType(f.dynamicForms.inputType)
            if (f.dynamicForms.min != null)      min(      f.dynamicForms.min)
            if (f.dynamicForms.max != null)      max(      f.dynamicForms.max)
            if (f.dynamicForms.value != null)    value(    f.dynamicForms.value)
            if (f.dynamicForms.mask != null)     mask(     f.dynamicForms.mask)
            if (f.dynamicForms.pattern != null)  pattern(  f.dynamicForms.pattern)
            if (f.dynamicForms.errmsg != null)   errmsg(   f.dynamicForms.errmsg)
                        
            if (f.dynamicForms.updateScriptRef != null) additional{ updateScriptRef(f.dynamicForms.updateScriptRef) }
            if (f.dynamicForms.updateQuerytRef != null) additional{ updateQuerytRef(f.dynamicForms.updateQuerytRef) }
        }
    }

    private void setAppinfoListOfValues(xsd, Field f) {
        xsd.listOfValues {
            if (f.listOfValues.scriptRef)       scriptRef(      f.listOfValues.scriptRef)
            if (f.listOfValues.queryRef)        queryRef(       f.listOfValues.queryRef)
            if (f.listOfValues.propertyNames)   propertyNames(  f.listOfValues.propertyNames)
            if (f.listOfValues.inputName)       inputName(      f.listOfValues.inputName)
            if (f.listOfValues.values)          values(         f.listOfValues.values.join(','))
        }
    }

    private void buildField(xsd, Field f) {
        Logger.msg 1, "SchemaDelegate.buildField() - Field: $f.name"

        //TODO: implement support for this combination - see issue 129
        if (((f.attributes || f.unit) && hasRestrictions(f)) || (f.attributes && f.unit))
            throw new InvalidDataException('Field cannot have attributes, unit and restrictions at the same time')

        xsd.'xs:element'(name: f.name, type: fieldType(f), 'default': f.defaultVal, minOccurs: f.minOccurs, maxOccurs: f.maxOccurs) {
            if(f.documentation || f.dynamicForms || f.listOfValues) {
                'xs:annotation' {
                    if (f.documentation) 'xs:documentation'(f.documentation) 
                    if (f.dynamicForms || f.listOfValues) {
                        'xs:appinfo' {
                            if (f.dynamicForms) setAppinfoDynamicForms(xsd, f)
                            if (f.listOfValues) setAppinfoListOfValues(xsd, f)
                        }
                    }
                }
            }

            if(f.attributes) {
                'xs:complexType' {
                    'xs:simpleContent' {
                        'xs:extension'(base: f.type) {
                            f.attributes.each { Attribute a -> buildAtribute(xsd, a) }
                        }
                    }
                }
            }
            else if(f.unit) {
                'xs:complexType' {
                    'xs:simpleContent' {
                        'xs:extension'(base: f.type) {
                            'xs:attribute'(name:"unit", type: (!f.unit.values ? 'xs:string' : ''), 'default': f.unit.defaultVal, 'use': (f.unit.defaultVal ? "optional": "required")) {
                                if(f.unit.values) {
                                    buildRestriction(xsd, 'xs:string', f.unit.values, null, null, null, null)
                                }
                            }
                        }
                    }
                }
            }
            else if(hasRestrictions(f)) {
                buildRestriction(xsd, f.type, f.values, f.pattern, f, f.totalDigits, f.fractionDigits)
            }
        }
    }


    private void buildAnyField(xsd, AnyField any) {
        Logger.msg 1, "SchemaDelegate.buildAnyField()"
        
        xsd.'xs:any'(minOccurs: any.minOccurs, maxOccurs: any.maxOccurs, processContents: any.processContents)
    }

    private void buildRestriction(xsd, String type, List values, String pattern, Attribute a, Integer totalDigits, Integer fractionDigits) {
        Logger.msg 1, "SchemaDelegate.buildRestriction() - type:$type"

        xsd.'xs:simpleType' {
            'xs:restriction'(base: type) {
                if (values) values.each { 'xs:enumeration'(value: it) }

                if (pattern != null) 'xs:pattern'(value: pattern)

                if (a && a.minInclusive != null) 'xs:minInclusive'(value: a.minInclusive)
                if (a && a.minExclusive != null) 'xs:minExclusive'(value: a.minExclusive)
                if (a && a.maxInclusive != null) 'xs:maxInclusive'(value: a.maxInclusive)
                if (a && a.maxExclusive != null) 'xs:maxExclusive'(value: a.maxExclusive)

                if (totalDigits    != null) 'xs:totalDigits'(value: totalDigits)
                if (fractionDigits != null) 'xs:fractionDigits'(value: fractionDigits)
             }
        }
    }
}
