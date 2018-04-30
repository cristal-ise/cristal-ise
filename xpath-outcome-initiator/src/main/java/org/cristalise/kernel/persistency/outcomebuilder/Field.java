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
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.persistency.outcomebuilder.field.StringField;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ElementDecl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class Field extends OutcomeStructure {

    StringField   myFieldInstance = null;
    AttributeList myAttributes;
    Text          textNode;

    public Field(ElementDecl model) {
        super(model);

        // field can have attributes
        myAttributes = new AttributeList(model);

        Logger.msg(8, "Field() - name:"+model.getName()+" optional:" + isOptional());
        
        // skipping optional fields
        //if (isOptional()) return;

        try {
            myFieldInstance = StringField.getField(model);
            Logger.msg(6, "Field() - name:" + model.getName() + " type: "+myFieldInstance.getClass().getSimpleName());
        }
        catch (StructuralException e) {
            // no base type for field - only attributes
            myFieldInstance = null;
        }
    }

    public AttributeList getAttributes() {
        return myAttributes;
    }

    @Override
    public void addStructure(OutcomeStructure newElement) throws OutcomeBuilderException {
        throw new StructuralException("Field "+model.getName()+" cannot have child structures");
    }

    @Override
    public void addInstance(Element newElement, Document parentDoc) throws OutcomeBuilderException {
        Logger.msg(6, "Field.addInstance() - field:"+newElement.getTagName());

        // Set attributes first
        myAttributes.addInstance(newElement, parentDoc);

        if (this.myElement != null) throw new CardinalException("Field '"+this.getName()+"' cannot repeat");

        this.myElement = newElement;

        try {
            if (myFieldInstance == null) {
                Logger.warning("Field.addInstance() - Field '"+newElement.getTagName()+"' should be empty. Discarding contents.");
            }
            else {
                if (newElement.hasChildNodes())
                    textNode = (Text)newElement.getFirstChild();
                else {
                    textNode = parentDoc.createTextNode(getDefaultValue());
                    newElement.appendChild(textNode);
                }

                myFieldInstance.setData(textNode);
            }
        }
        catch (ClassCastException ex) {
            throw new StructuralException("First child node of Field " + this.getName() + " was not Text: "+newElement.getFirstChild().getNodeType());
        }
    }

    private void createOptinalElement(Element parent, String value) throws StructuralException {
        Logger.msg(5, "Field.createOptinalElement() - name: "+model.getName());

        if (myFieldInstance == null) myFieldInstance = StringField.getField(model);

        if (myElement == null) myElement = parent.getOwnerDocument().createElement(model.getName());
        parent.appendChild(myElement);

        textNode = parent.getOwnerDocument().createTextNode(getDefaultValue());
        myElement.appendChild(textNode);

        textNode.setData(value);
        myFieldInstance.setData(textNode);
    }

    private String getJsonValue(Object jsonValue) {
        Logger.msg("+++ jsonValue:" +jsonValue + " class:" + jsonValue.getClass().getSimpleName());
        if (jsonValue instanceof BigDecimal) {
            BigDecimal decimalVal = (BigDecimal)jsonValue;
            
            decimalVal = decimalVal.setScale(2, RoundingMode.HALF_UP);
            
            return decimalVal.toString();
        }
        else
            return jsonValue.toString();
    }

    @Override
    public void addJsonInstance(Element parent, String name, Object json) throws OutcomeBuilderException {
        Logger.msg(5, "Field.addJsonInstance() - name:'" + name + "'");

        if (json instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject)json;

            for (String key: jsonObj.keySet()) {
                if (myAttributes.hasAttributeDecl(key)) {
                    myAttributes.addJsonInstance(myElement, key, jsonObj.get(key));
                }
                else {
                    if (key.equals("content")) {
                        if (myFieldInstance == null) createOptinalElement(parent, getJsonValue(jsonObj.get(key)));
                        else                         myFieldInstance.setData(getJsonValue(jsonObj.get(key).toString()));
                    }
                    else {
                        // this should never happen
                        throw new InvalidOutcomeException("JSON key '"+key +"' is not an attribute of Outcome field:"+name);
                    }
                }
            }
        }
        else if (json instanceof JSONArray) {
            throw new UnsupportedOperationException("Field name '" + name + "' contains an ARRAY");
        }
        else {
            if (myFieldInstance == null || myElement == null) createOptinalElement(parent, getJsonValue(json));
            else                                              myFieldInstance.setData(getJsonValue(json));
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
        Logger.msg(6, "Field.initiNew() - Creating '"+this.getName()+"'");

        // make a new Element
        myElement = rootDocument.createElement(this.getName());

        // see if there is a default/fixed value
        if (myFieldInstance != null) {
            // populate
            String defaultVal = getDefaultValue();
            textNode = rootDocument.createTextNode(defaultVal);
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
    public Object generateNgDynamicForms() {
        String defVal = getDefaultValue();

        JSONObject fieldJson = myFieldInstance.generateNgDynamicForms();

        if (StringUtils.isNotBlank(defVal))
            fieldJson.put("value", defVal);

        return fieldJson;
    }
}
