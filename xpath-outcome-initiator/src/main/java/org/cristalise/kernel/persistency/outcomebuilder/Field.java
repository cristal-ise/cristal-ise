/**
 * This file is part of the CRISTAL-iSE XPath Outcome Initiator module.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.persistency.outcomebuilder;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.persistency.outcomebuilder.field.StringField;
import org.exolab.castor.xml.schema.ElementDecl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Accessors(prefix = "my") @Getter
public class Field extends OutcomeStructure {

    StringField   myFieldInstance = null;
    AttributeList myAttributes;

    public Field(ElementDecl model) {
        super(model);

        // field can have attributes
        myAttributes = new AttributeList(model);

        log.debug("name:"+model.getName()+" optional:" + isOptional());

        try {
            myFieldInstance = StringField.getField(model);
            log.debug("name:" + model.getName() + " type: "+myFieldInstance.getClass().getSimpleName());
        }
        catch (StructuralException e) {
            // no base type for field - only attributes
            myFieldInstance = null;
        }
    }

    @Override
    public void addStructure(OutcomeStructure newElement) throws OutcomeBuilderException {
        throw new StructuralException("Field "+model.getName()+" cannot have child structures");
    }

    private Text getInitalisedTextNode(Element newElement, Document parentDoc) throws OutcomeBuilderException {
        try {
            Text textNode = null;
            if (newElement.hasChildNodes())
                textNode = (Text)newElement.getFirstChild();
            else {
                textNode = parentDoc.createTextNode(getDefaultValue());
                newElement.appendChild(textNode);
            }
            return textNode;
        }
        catch (ClassCastException ex) {
            throw new StructuralException("First child node of Field " + this.getName() + " was not Text: "+newElement.getFirstChild().getNodeType());
        }
    }

    @Override
    public void addInstance(Element newElement, Document parentDoc) throws OutcomeBuilderException {
        log.debug("addInstance() - field:"+newElement.getTagName());

        // Set attributes first
        myAttributes.addInstance(newElement, parentDoc);

        if (this.myElement != null) {
            if (!isMultiple(model)) throw new CardinalException("Field '"+this.getName()+"' cannot repeat");

            //set the value in the myFieldInstance which has to be a ComboField
            myFieldInstance.setData(getInitalisedTextNode(newElement, parentDoc));
        }
        else {
            this.myElement = newElement;

            if (myFieldInstance == null) {
                // should this be an error instead????
                log.warn("addInstance() - Field '"+newElement.getTagName()+"' should be empty. Discarding contents.");
            }
            else {
                myFieldInstance.setData(getInitalisedTextNode(newElement, parentDoc));
            }
        }
    }

//    private void createOptinalElement(Element parent) throws StructuralException {
//        log.debug("createOptinalElement() - name: "+model.getName());
//
//        if (myFieldInstance == null) myFieldInstance = StringField.getField(model);
//
//        if (myElement == null) {
//            myElement = parent.getOwnerDocument().createElement(model.getName());
//            parent.appendChild(myElement);
//        }
//
//        if (myFieldInstance.getData() == null) {
//            textNode = parent.getOwnerDocument().createTextNode(getDefaultValue());
//            myElement.appendChild(textNode);
//            myFieldInstance.setData(textNode);
//        }
//    }

    @Override
    public void addJsonInstance(OutcomeStructure parentStruct, Element parentElement, String name, Object json) throws OutcomeBuilderException {
        log.debug("addJsonInstance() - name:'" + name + "'");

        if (json instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject)json;

            for (String key: jsonObj.keySet()) {
                if (myAttributes.hasAttributeDecl(key)) {
                    myAttributes.addJsonInstance(this, myElement, key, jsonObj.get(key));
                }
                else {
                    //'content' is the field name used by the org.json.XML to handle value of Element with attributes
                    if (key.equals("content")) {
                        setJsonValue(parentStruct, parentElement, name, jsonObj.get(key));
                    }
                    else {
                        // this should never happen
                        throw new InvalidOutcomeException("JSON key '"+key +"' is not an attribute of Outcome field:"+name);
                    }
                }
            }
        }
        else if (json instanceof JSONArray) {
            log.warn("addJsonInstance() - Field name '" + name + "' contains an ARRAY. Parsing ARRAY to String");
            setJsonValue(parentStruct, parentElement, name, json.toString());
        }
        else {
            //json variable is not JSOObject nor JSONArray, so handle it as a value
            setJsonValue(parentStruct, parentElement, name, json);
        }
    }

    public boolean isNullJsonValue(Object val) {
        return val == null || StringUtils.isBlank(val.toString()) || "null".equals(val.toString()) || JSONObject.NULL.equals(val);
    }

    private void setJsonValue (OutcomeStructure parentStruct, Element parentElement, String name, Object val) throws OutcomeBuilderException {
        if (isOptional() && isNullJsonValue(val)) {
            log.debug("addJsonInstance() - skipping empty optional element:"+getName());
        }
        else {
            if (isOptional()) parentStruct.createChildElement(parentElement.getOwnerDocument(), name);
            myFieldInstance.setValue(val);
        }
    }

    @Override
    public String validateStructure() {
        myAttributes.validateAttributes();

        if (myFieldInstance != null) myFieldInstance.updateNode();

        Text contents = (Text)myElement.getFirstChild();

        // empty - should remove if optional
        if (!myElement.hasAttributes() && model.getMinOccurs() < 1 && (contents == null || contents.getData().length() == 0))
            myElement.getParentNode().removeChild(myElement);

        return null;
    }

    @Override
    public Element initNew(Document rootDocument) {
        log.debug("initiNew() - Creating '"+this.getName()+"'");

        // make a new Element
        myElement = rootDocument.createElement(this.getName());

        // see if there is a default/fixed value
        if (myFieldInstance != null) {
            // populate
            String defaultVal = getDefaultValue();
            Text textNode = rootDocument.createTextNode(defaultVal);
            myElement.appendChild(textNode);
            myFieldInstance.setData(textNode);
        }

        // set up attributes
        myAttributes.initNew(myElement);

        return myElement;
    }

    private String getDefaultValue() {
        String defaultValue = model.getFixedValue();
        if (defaultValue == null) defaultValue = model.getDefaultValue();
        if (defaultValue == null) defaultValue = myFieldInstance.getDefaultValue();

        return defaultValue;
    }

    @Override
    public OutcomeStructure getChildModelElement(String name) {
        //TODO implement lookup in attributes
        return null;
    }

    @Override
    public void exportViewTemplate(Writer template) throws IOException {
        String type = myFieldInstance.getClass().getSimpleName();
        template.write("<Field name='"+model.getName()+"' type='"+type+"'/>");
    }

    @Override
    public JSONObject generateNgDynamicFormsCls() {
        return myFieldInstance.generateNgDynamicFormsCls();
    }

    @Override
    public Object generateNgDynamicForms(Map<String, Object> inputs) {
        String defVal = getDefaultValue();

        JSONObject fieldJson = myFieldInstance.generateNgDynamicForms(inputs);

        if (StringUtils.isNotBlank(defVal)) fieldJson.put("value", defVal);
        if (StringUtils.isNotBlank(help))   myFieldInstance.getAdditionalConfigNgDynamicForms(fieldJson).put("tooltip", help.trim());

        // dynamicForms.additional fields provided in the schema can overwrite default values (check ListOfValues.editable)
        myFieldInstance.updateWithAdditional(fieldJson);

        return fieldJson;
    }

    /**
     * Finds the named Element in the named AppInfo node, and returns the value
     * 
     * @param nodeName the name of the AppInfo node 
     * @param elementName the name of the Element in the named AppInfo node
     * @return value of the named Element in the named AppInfo node if exists, otherwise returns null.
     */
    public String getAppInfoNodeElementValue(String nodeName, String elementName) {
        return StructureWithAppInfo.getAppInfoNodeElementValue(model, nodeName, elementName);
    }
}
