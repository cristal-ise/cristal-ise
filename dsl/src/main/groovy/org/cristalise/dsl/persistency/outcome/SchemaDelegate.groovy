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

import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.property.BuiltInItemProperties
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.property.PropertyUtility
import org.cristalise.kernel.scripting.Script

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder


/**
 *
 */
@Slf4j @CompileStatic
class SchemaDelegate {

    String  name
    Integer version

    String xsdString
    
    Map<String, Script> expressionScripts = [:]
    Map<String, List<String>> expressionScriptsInputFields = [:]
    
    private updateScriptReferences(Struct s) {
        if (!s || !s.fields) return

        s.fields.each { name, f ->
            if (f.expression) this.generateExpressionScript(s, f)
        }

        expressionScriptsInputFields.each { scriptName, inputFields ->
            inputFields.each { fieldName ->
                def inputField = s.fields[fieldName]
                if (inputField.dynamicForms == null) inputField.dynamicForms = new DynamicForms()
                inputField.dynamicForms.updateScriptRef = expressionScripts[scriptName]
            }
        }
    }

    @CompileDynamic
    public void processClosure(Closure cl) {
        assert cl, "Schema only works with a valid Closure"

        def objBuilder = new ObjectGraphBuilder()
        objBuilder.setChildPropertySetter(new DSLPropertySetter())
        objBuilder.classLoader = this.class.classLoader
        objBuilder.classNameResolver = 'org.cristalise.dsl.persistency.outcome'

        cl.delegate = objBuilder

        Struct s = cl()
        updateScriptReferences(s)
        xsdString = buildXSD(s)
    }

    public void processTabularData(TabularGroovyParser parser) {
        def tsb = new TabularSchemaBuilder()
        Struct s = tsb.build(parser)
        updateScriptReferences(s)
        xsdString = buildXSD(s)
    }

    @CompileDynamic
    public String buildXSD(Struct s) {
        if(!s) throw new InvalidDataException("Schema cannot be built from empty declaration")

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
        log.info "buildStruct() - Struct: $s.name"
        xsd.'xs:element'(name: s.name, minOccurs: s.minOccurs, maxOccurs: s.maxOccurs) {
            if(s.documentation || s.dynamicForms) {
                'xs:annotation' { 
                    if (s.documentation) {
                        'xs:documentation'(s.documentation)
                    }
                    if (s.dynamicForms) {
                        'xs:appinfo' {
                            dynamicForms {
                                if (s.dynamicForms.width)     width(     s.dynamicForms.width)
                                if (s.dynamicForms.label)     label(     s.dynamicForms.label)
                                if (s.dynamicForms.container) container( s.dynamicForms.container)
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
            if (s.fields.containsKey(name))  this.buildField(xsd, s.fields[name])
            if (s.structs.containsKey(name)) this.buildStruct(xsd, s.structs[name])
        }
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

    @CompileDynamic
    private void buildAtribute(MarkupBuilder xsd, Attribute a) {
        log.info "buildAtribute() - attribute: $a.name"

        if (a.documentation) throw new InvalidDataException('Attribute cannot define documentation')

        xsd.'xs:attribute'(name: a.name, type: attributeType(a), 'default': a.defaultVal, 'use': (a?.required ? "required": "")) {
            if(hasRestrictions(a)) {
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

                if (f.reference.itemType instanceof String)  {
                    itemRef = f.reference.itemType
                }
                else if (f.reference.itemType instanceof PropertyDescriptionList) {
                    def propDesc = (PropertyDescriptionList) f.reference.itemType
                    itemRef = PropertyUtility.getDefaultValue(propDesc.list, BuiltInItemProperties.TYPE.getName())

                    if (!itemRef) throw new InvalidDataException("Property called '${BuiltInItemProperties.TYPE}' is missing")
                }
                else
                    throw new InvalidDataException("itemType must be a String or PropertyDescriptionList")

                itemType(itemRef)
            }
        }
    }

    private void generateExpressionScript(Struct s, Field f) {
        log.info('generateExpressionScript(field:{}) - script:{}', f.name, f.expression.name)

        def script = new Script('groovy', f.expression.generateUpdateScript(s, f, name, version))
        // this constructor adds a default output which is not needed
        script.getOutputParams().clear()

        script.name = f.expression.name
        script.version = f.expression.version
        script.addInputParam(name, 'org.json.JSONObject')
        script.addInputParam('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
        script.addInputParam('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
        script.addOutput(name+'Xml', 'java.lang.String')

        expressionScripts[f.expression.name] = script
        expressionScriptsInputFields[f.expression.name] = f.expression.inputFields
    }

    @CompileDynamic
    private void buildField(MarkupBuilder xsd, Field f) {
        log.info "buildField() - Field: $f.name"

        //TODO: implement support for this combination - see issue 129
        if (((f.attributes || f.unit) && hasRestrictions(f)) || (f.attributes && f.unit))
            throw new InvalidDataException('Field cannot have attributes, unit and restrictions at the same time')

        xsd.'xs:element'(name: f.name, type: fieldType(f), 'default': f.defaultVal, minOccurs: f.minOccurs, maxOccurs: f.maxOccurs) {
            if(f.documentation || this.hasAppinfoNodes(f)) {
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

            if(f.attributes) {
                'xs:complexType' {
                    'xs:simpleContent' {
                        'xs:extension'(base: f.type) {
                            f.attributes.each { Attribute a -> this.buildAtribute(xsd, a) }
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
            else if(hasRestrictions(f)) {
                buildRestriction(xsd, f)
            }
        }
    }

    @CompileDynamic
    private void buildAnyField(MarkupBuilder xsd, AnyField any) {
        log.info "buildAnyField()"

        xsd.'xs:any'(minOccurs: any.minOccurs, maxOccurs: any.maxOccurs, processContents: any.processContents)
    }

    @CompileDynamic
    private void buildRestriction(MarkupBuilder xsd, Attribute fieldOrAttr) {
        log.info "buildRestriction() - type:$fieldOrAttr.type"

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
