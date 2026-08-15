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
package org.cristalise.dsl.persistency.outcome.generator

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.cristalise.dsl.persistency.outcome.AnyField
import org.cristalise.dsl.persistency.outcome.Attribute
import org.cristalise.dsl.persistency.outcome.Field
import org.cristalise.dsl.persistency.outcome.Struct
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.property.BuiltInItemProperties
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.property.PropertyUtility

/**
 * Generator that transforms a Struct AST into an XML Schema (XSD) string.
 */
@Slf4j @CompileStatic
class XsdSchemaGenerator implements SchemaGenerator<String> {

    @Override
    @CompileDynamic
    String generate(Struct s, SchemaContext context) {
        if (!s) throw new InvalidDataException("Schema cannot be built from empty declaration")

        def writer = new StringWriter()
        def xsd = new MarkupBuilder(writer)

        xsd.setOmitEmptyAttributes(true)
        xsd.setOmitNullAttributes(true)

        xsd.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")

        xsd.'xs:schema'('xmlns:xs': 'http://www.w3.org/2001/XMLSchema') {
            buildStruct(xsd, s)
        }

        return writer.toString()
    }

    @CompileDynamic
    private void buildStruct(MarkupBuilder xsd, Struct s) {
        log.debug "buildStruct() - Struct: $s.name"
        xsd.'xs:element'(name: s.name, minOccurs: s.minOccurs, maxOccurs: s.maxOccurs) {
            if (s.documentation || s.dynamicForms) {
                'xs:annotation' {
                    if (s.documentation) {
                        'xs:documentation'(s.documentation)
                    }
                    if (s.dynamicForms) {
                        'xs:appinfo' {
                            dynamicForms {
                                if (s.dynamicForms.width)            width(    s.dynamicForms.width)
                                if (s.dynamicForms.label)            label(    s.dynamicForms.label)
                                if (s.dynamicForms.container)        container(s.dynamicForms.container)
                                if (s.dynamicForms.hidden   != null) hidden(   s.dynamicForms.hidden)
                                if (s.dynamicForms.required != null) required( s.dynamicForms.required)
                            }
                        }
                    }
                }
            }

            'xs:complexType' {
                if (s.orderOfElements || s.anyField) {
                    if (s.useSequence) {
                        'xs:sequence' {
                            this.buildStructElements(xsd, s)
                        }
                    }
                    else {
                        'xs:all'(minOccurs: '0') {
                            this.buildStructElements(xsd, s)
                        }
                    }
                }

                if (s.attributes) {
                    s.attributes.each { Attribute a -> this.buildAtribute(xsd, a) }
                }
            }
        }
    }

    private void buildStructElements(MarkupBuilder xsd, Struct s) {
        s.orderOfElements.each { String name ->
            if      (s.fields.containsKey(name))  this.buildField(xsd, s.fields[name])
            else if (s.structs.containsKey(name)) this.buildStruct(xsd, s.structs[name])
        }
        // xsd:any must be the last in sequence
        if (s.anyField) this.buildAnyField(xsd, s.anyField)
    }

    private boolean hasRangeConstraints(Attribute a) {
        return a.minInclusive != null || a.maxInclusive != null || a.minExclusive != null || a.maxExclusive != null
    }

    private boolean hasLengthConstraints(Attribute a) {
        return a.length != null || a.maxLength != null || a.minLength != null
    }

    private boolean hasNumericConstraints(Attribute a) {
        return a.totalDigits != null || a.fractionDigits != null
    }

    private boolean hasRestrictions(Attribute a) {
        return a.values || a.pattern || hasRangeConstraints(a) || hasNumericConstraints(a) || hasLengthConstraints(a)
    }

    private boolean hasAppinfoNodes(Field f) {
        return f.dynamicForms || f.listOfValues || f.reference
    }

    private String fieldType(Field f) {
        if (hasRestrictions(f) || f.attributes || f.unit) return ''
        else                                              return f.type
    }

    private String attributeType(Attribute a) {
        if (hasRestrictions(a)) return ''
        else                    return a.type
    }

    @CompileDynamic
    private void buildAtribute(MarkupBuilder xsd, Attribute a) {
        log.debug "buildAtribute() - attribute: $a.name"

        if (a.documentation) throw new InvalidDataException('Attribute cannot define documentation')

        xsd.'xs:attribute'(name: a.name, type: attributeType(a), 'default': a.defaultVal, 'use': (a?.required ? "required": "")) {
            if (hasRestrictions(a)) {
                buildRestriction(xsd, a)
            }
        }
    }

    @CompileDynamic
    private void setAppinfoDynamicForms(xsd, Field f) {
        xsd.dynamicForms {
            if (f.dynamicForms.hidden   != null)             hidden(      f.dynamicForms.hidden)
            if (f.dynamicForms.required != null)             required(    f.dynamicForms.required)
            if (f.dynamicForms.disabled != null)             disabled(    f.dynamicForms.disabled)
            if (f.dynamicForms.multiple != null)             multiple(    f.dynamicForms.multiple)
            if (f.dynamicForms.label)                        label(       f.dynamicForms.label)
            if (f.dynamicForms.placeholder)                  placeholder( f.dynamicForms.placeholder)
            if (f.dynamicForms.type)                         type(        f.dynamicForms.type)
            if (f.dynamicForms.inputType)                    inputType(   f.dynamicForms.inputType)
            if (f.dynamicForms.min != null)                  min(         f.dynamicForms.min)
            if (f.dynamicForms.max != null)                  max(         f.dynamicForms.max)
            if (f.dynamicForms.value)                        value(       f.dynamicForms.value)
            if (f.dynamicForms.mask)                         mask(        f.dynamicForms.mask)
            if (f.dynamicForms.autoComplete)                 autoComplete(f.dynamicForms.autoComplete)
            if (f.dynamicForms.pattern)                      pattern(     f.dynamicForms.pattern)
            if (f.dynamicForms.errmsg)                       errmsg(      f.dynamicForms.errmsg)
            if (f.dynamicForms.showSeconds != null)          showSeconds( f.dynamicForms.showSeconds)
            if (f.dynamicForms.container)                    container(   f.dynamicForms.container)
            if (f.dynamicForms.control)                      control(     f.dynamicForms.control)
            if (f.dynamicForms.labelGrid)                    labelGrid(   f.dynamicForms.labelGrid)
            if (f.dynamicForms.hideOnDateTimeSelect != null) hideOnDateTimeSelect( f.dynamicForms.hideOnDateTimeSelect)
            if (f.dynamicForms.precision)                    precision(   f.dynamicForms.precision)
            if (f.dynamicForms.scale)                        scale(       f.dynamicForms.scale)

            if (f.hasAdditional()) {
                additional {
                    if (f.dynamicForms.additional) {
                        f.dynamicForms.additional.fields.each { key, value -> "$key"(value) }
                    }
                    if (f.dynamicForms.updateScriptRef != null) updateScriptRef(f.dynamicForms.getUpdateScriptRefString())
                    if (f.dynamicForms.updateQuerytRef != null) updateQuerytRef(f.dynamicForms.getUpdateQueryRefString())
                    if (f.dynamicForms.warning != null) {
                        warning {
                            if (f.dynamicForms.warning.pattern)    pattern(f.dynamicForms.warning.pattern)
                            if (f.dynamicForms.warning.message)    message(f.dynamicForms.warning.message)
                            if (f.dynamicForms.warning.expression) expression {
                                mkp.yieldUnescaped("<![CDATA[ ${f.dynamicForms.warning.expression}]]>")
                            }
                        }
                    }
                    if (f.dynamicForms.updateFields) updateFields(f.dynamicForms.updateFields.join(','))
                }
            }

            if (f.isFileUpload()) {
                if (f.dynamicForms.htmlAccept != null) accept(f.dynamicForms.htmlAccept)
            }
        }
    }

    @CompileDynamic
    private void setAppinfoListOfValues(xsd, Field f) {
        xsd.listOfValues {
            if (f.listOfValues.scriptRef)       scriptRef(      f.listOfValues.getScriptRefString())
            if (f.listOfValues.queryRef)        queryRef(       f.listOfValues.getQueryRefString())
            if (f.listOfValues.propertyNames)   propertyNames(  f.listOfValues.propertyNames)
            if (f.listOfValues.inputName)       inputName(      f.listOfValues.inputName)
            if (f.listOfValues.values)          values(         f.listOfValues.values.join(','))
        }
    }

    @CompileDynamic
    private void setAppinfoReference(xsd, Field f) {
        xsd.reference {
            if (f.reference.itemType) {
                def itemRef = ""

                if (f.reference.itemType instanceof String) {
                    itemRef = f.reference.itemType
                }
                else if (f.reference.itemType instanceof PropertyDescriptionList) {
                    def propDesc = (PropertyDescriptionList) f.reference.itemType
                    itemRef = PropertyUtility.getDefaultValue(propDesc.list, BuiltInItemProperties.TYPE.getName())

                    if (!itemRef) throw new InvalidDataException("Property called '${BuiltInItemProperties.TYPE}' is missing")
                }
                else {
                    throw new InvalidDataException("itemType must be a String or PropertyDescriptionList")
                }

                itemType(itemRef)

                if (f.reference.collectionName) collectionName(f.reference.collectionName)
            }
            else if (f.reference.collectionName) {
                throw new InvalidDataException("itemType must be specified as well")
            }
        }
    }

    @CompileDynamic
    private void buildField(MarkupBuilder xsd, Field f) {
        log.debug "buildField() - Field: $f.name"

        if (((f.attributes || f.unit) && hasRestrictions(f)) || (f.attributes && f.unit))
            throw new InvalidDataException('Field cannot have attributes, unit and restrictions at the same time')

        xsd.'xs:element'(name: f.name, type: fieldType(f), 'default': f.defaultVal, minOccurs: f.minOccurs, maxOccurs: f.maxOccurs) {
            if (f.documentation || this.hasAppinfoNodes(f)) {
                'xs:annotation' {
                    if (f.documentation) 'xs:documentation'(f.documentation)
                    if (this.hasAppinfoNodes(f)) {
                        'xs:appinfo' {
                            if (f.dynamicForms) this.setAppinfoDynamicForms(xsd, f)
                            if (f.listOfValues) this.setAppinfoListOfValues(xsd, f)
                            if (f.reference)    this.setAppinfoReference(xsd, f)
                        }
                    }
                }
            }

            if (f.attributes) {
                'xs:complexType' {
                    'xs:simpleContent' {
                        'xs:extension'(base: f.type) {
                            f.attributes.each { Attribute a -> this.buildAtribute(xsd, a) }
                        }
                    }
                }
            }
            else if (f.unit) {
                'xs:complexType' {
                    'xs:simpleContent' {
                        'xs:extension'(base: f.type) {
                            'xs:attribute'(name:"unit", type: (!f.unit.values ? 'xs:string' : ''), 'default': f.unit.defaultVal, 'use': (f.unit.defaultVal ? "optional": "required")) {
                                if (f.unit.values) {
                                    'xs:simpleType' {
                                        'xs:restriction'(base: 'xs:string') {
                                            f.unit.values.each { 'xs:enumeration'(value: it) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (hasRestrictions(f)) {
                buildRestriction(xsd, f)
            }
        }
    }

    @CompileDynamic
    private void buildAnyField(MarkupBuilder xsd, AnyField any) {
        log.debug "buildAnyField()"

        xsd.'xs:any'(minOccurs: any.minOccurs, maxOccurs: any.maxOccurs, processContents: any.processContents)
    }

    @CompileDynamic
    private void buildRestriction(MarkupBuilder xsd, Attribute fieldOrAttr) {
        log.debug "buildRestriction() - type:$fieldOrAttr.type"

        xsd.'xs:simpleType' {
            'xs:restriction'(base: fieldOrAttr.type) {
                if (fieldOrAttr.values) fieldOrAttr.values.each { 'xs:enumeration'(value: it) }

                if (fieldOrAttr.pattern != null) 'xs:pattern'(value: fieldOrAttr.pattern)

                if (fieldOrAttr.minInclusive != null) 'xs:minInclusive'(value: fieldOrAttr.minInclusive)
                if (fieldOrAttr.minExclusive != null) 'xs:minExclusive'(value: fieldOrAttr.minExclusive)
                if (fieldOrAttr.maxInclusive != null) 'xs:maxInclusive'(value: fieldOrAttr.maxInclusive)
                if (fieldOrAttr.maxExclusive != null) 'xs:maxExclusive'(value: fieldOrAttr.maxExclusive)

                if (fieldOrAttr.totalDigits    != null) 'xs:totalDigits'(value: fieldOrAttr.totalDigits)
                if (fieldOrAttr.fractionDigits != null) 'xs:fractionDigits'(value: fieldOrAttr.fractionDigits)

                if (fieldOrAttr.length    != null) 'xs:length'(value: fieldOrAttr.length)
                if (fieldOrAttr.minLength != null) 'xs:minLength'(value: fieldOrAttr.minLength)
                if (fieldOrAttr.maxLength != null) 'xs:maxLength'(value: fieldOrAttr.maxLength)
            }
        }
    }
}
