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
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataRecord extends OutcomeStructure {

    AttributeList myAttributes;
    Document      parentDoc;

    public DataRecord(ElementDecl model) throws OutcomeBuilderException {
        super(model);
        log.debug("ctor() - name:{} optional:{} isAnyType:{}", model.getName(), isOptional(), isAnyType());
        setup();
    }

    public synchronized void activate() {
        try {
            setup();
            if (myElement != null) populateInstance();
        }
        catch (OutcomeBuilderException ex) {
            log.error("", ex);
        }
    }

    private void setup() throws OutcomeBuilderException {
        // attributes at the top
        myAttributes = new AttributeList(model);

        ComplexType elementType;
        try {
            elementType = (ComplexType) model.getType();
        }
        catch (ClassCastException e) {
            throw new StructuralException("DataRecord created with non-ComplexType");
        }

        // loop through all schema sub-elements
        enumerateElements(elementType);
    }

    @Override
    public void addStructure(OutcomeStructure newElement) throws OutcomeBuilderException {
        if (newElement == null) return;
        super.addStructure(newElement);

        //FIXME: perhaps this is just a leftover from the GUI code
        if (newElement instanceof DataRecord) {
            //DataRecord newRecord = (DataRecord) newElement;
        }
    }

    @Override
    public void addInstance(Element newElement, Document parentDoc) throws OutcomeBuilderException {
        log.debug("addInstance() - name:" + newElement.getTagName());

        if (this.myElement != null) throw new CardinalException("DataRecord " + this.getName() + " cannot repeat.");

        this.myElement = newElement;
        this.parentDoc = parentDoc;

        populateInstance();
    }

    private void populateInstance() throws StructuralException, OutcomeBuilderException {
        // First populate attributes
        myAttributes.addInstance(myElement, parentDoc);

        NodeList childElements = myElement.getChildNodes();

        for (int i = 0; i < childElements.getLength(); i++) {
            // ignore any Node (e.g. Text) which are not Element type
            if (!(childElements.item(i) instanceof Element)) continue;

            Element childElement = (Element) childElements.item(i);

            // find the child structure with this name
            OutcomeStructure childStructure = subStructure.get(childElement.getTagName());

            if (childStructure == null)
                throw new StructuralException("DR " + model.getName() + " not expecting child element with name '" + childElement.getTagName() + "'");

            childStructure.addInstance(childElement, parentDoc);
        }

        // make sure any dimensions have the minimum
        for (String structureName : subStructure.keySet()) {
            OutcomeStructure childStructure = subStructure.get(structureName);
            int count = 0;

            if (childStructure instanceof Dimension) {
                Dimension childDimension = (Dimension) childStructure;
                childDimension.setParentElement(myElement);
                count = childDimension.getChildCount();
            }
            else
                count = childStructure.getElement() == null ? 0 : 1;

            int total = childStructure.getModel().getMinOccurs();

            for (int i = count; i < total; i++) {
                myElement.appendChild(childStructure.initNew(parentDoc));
            }
        }
    }

    @Override
    public void addJsonInstance(OutcomeStructure parentStruct, Element parentElement, String name, Object json) throws OutcomeBuilderException {
        log.debug("addJsonInstance() - name:'" + name + "'");
        JSONObject jsonObj = (JSONObject)json;

//        myElement = parentElement;

        if (!name.equals(model.getName())) throw new InvalidOutcomeException("Missmatch in names:" + name + "!=" + model.getName());

        //attributes first, order is not important
        for (String key: jsonObj.keySet()) {
            if (myAttributes.hasAttributeDecl(key)) {
                myAttributes.addJsonInstance(this, myElement, key, jsonObj.get(key));
            }
        }

        for (String elementName : subStructureOrder) {
            OutcomeStructure childStructure = subStructure.get(elementName);

            if (childStructure == null) throw new InvalidOutcomeException("DataRecord '" + name + "' doesn not have a field " + elementName + "'");

            //Optional element might not be present in the json
            if (jsonObj.has(elementName)) {
                childStructure.addJsonInstance(this, myElement, elementName, jsonObj.get(elementName));
            }
        }
    }

    @Override
    public Element initNew(Document rootDocument) {
        log.debug("initNew() - name:'" + model.getName() + "'");

        // make a new Element
        myElement = rootDocument.createElement(model.getName());

        // set up attributes
        myAttributes.initNew(myElement);

        // populate
        for (String elementName : subStructureOrder) {
            OutcomeStructure childStructure = subStructure.get(elementName);
            
            if (childStructure instanceof Field) {
                if (((Field)childStructure).isAnyField) continue;
            }
            else if (childStructure instanceof Dimension) {
                ((Dimension) childStructure).setParentElement(myElement);
            }

            for (int i = 0; i < childStructure.getModel().getMinOccurs(); i++) {
                myElement.appendChild(childStructure.initNew(rootDocument));
            }
        }

        return myElement;
    }

    @Override
    public void exportViewTemplate(Writer template) throws IOException {
        template.write("<FieldSet name='" + model.getName() + "'>");

        for (String elementName : subStructureOrder) subStructure.get(elementName).exportViewTemplate(template);

        template.write("</FieldSet>");
    }

    @Override
    public JSONObject generateNgDynamicFormsCls() {
        JSONObject drCls = new JSONObject();
        JSONObject drGrid = new JSONObject();
        
        StructureWithAppInfo appInfoer = new StructureWithAppInfo();
        appInfoer.setAppInfoDynamicFormsJson(model, drGrid, true);
        
        // Set default value when container is not defined
        if (!drGrid.has("container")) {
            drGrid.put("container", "ui-g-12");
        }

        drCls.put("grid", drGrid);

        if (!isRootElement)  {
            JSONObject drClass = new JSONObject();

            drClass.put("label", "formGroupLabel");
            drClass.put("container", "formGroupContainer");

            drCls.put("element", drClass);
        }
        return drCls;
    }

    @Override
    public Object generateNgDynamicForms(Map<String, Object> inputs) {
        JSONObject dr = new JSONObject();
        
//        dr.put("cls", generateNgDynamicFormsCls());
        dr.put("type",  "GROUP");
        dr.put("id",    model.getName());
        dr.put("name",  model.getName());
        //dr.put("required", !isOptional());

        JSONArray array = myAttributes.generateNgDynamicForms(inputs);

        for (String elementName : subStructureOrder) {
            Object formFregment = subStructure.get(elementName).generateNgDynamicForms(inputs);
            if (formFregment != null) array.put(formFregment);
        }

        if (!isRootElement && !dr.has("label")) {
            String label = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(model.getName()), " ");
            dr.put("label", label);
        }

        StructureWithAppInfo appInfoer = new StructureWithAppInfo();

        //This call could overwrite values set earlier
        appInfoer.setAppInfoDynamicFormsJson(model, dr, false);

        dr.put("group", array);

        return dr;
    }
}
