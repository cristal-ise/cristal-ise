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

import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DataRecord extends OutcomeStructure {

    AttributeList myAttributes;
    Document      parentDoc;

    public DataRecord(ElementDecl model) throws OutcomeBuilderException {
        super(model);
        setup();
    }

    public synchronized void activate() {
        try {
            setup();
            if (myElement != null) populateInstance();
        }
        catch (OutcomeBuilderException ex) {
            Logger.error(ex);
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
        Logger.msg(8, "DataRecord.addInstance() - name:" + newElement.getTagName());

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
    public void addJsonInstance(Element parent, String name, Object json) throws OutcomeBuilderException {
        Logger.msg(5, "DataRecord.addJsonInstance() - name:'" + name + "'");
        JSONObject jsonObj = (JSONObject)json;

        myElement = parent;

        if (!name.equals(model.getName())) throw new InvalidOutcomeException("Missmatch in names:" + name + "!=" + model.getName());

        //attributes first, order is not important
        for (String key: jsonObj.keySet()) {
            if (myAttributes.hasAttributeDecl(key)) {
                myAttributes.addJsonInstance(myElement, key, jsonObj.get(key));
            }
        }

        for (String elementName : subStructureOrder) {
            OutcomeStructure childStructure = subStructure.get(elementName);

            if (childStructure == null) throw new InvalidOutcomeException("DataRecord '" + name + "' doesn not have a field " + elementName + "'");

            //Optional element might not be present in the json
            if (jsonObj.has(elementName)) {
                childStructure.addJsonInstance(parent, elementName, jsonObj.get(elementName));
            }
        }
    }

    @Override
    public Element initNew(Document rootDocument) {
        Logger.msg(5, "DataRecord.initNew() - name:'" + model.getName() + "'");

        // make a new Element
        myElement = rootDocument.createElement(model.getName());

        // set up attributes
        myAttributes.initNew(myElement);

        // populate
        for (String elementName : subStructureOrder) {
            OutcomeStructure childStructure = subStructure.get(elementName);

            if (childStructure instanceof Dimension) ((Dimension) childStructure).setParentElement(myElement);

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

        template.write("</DataRecord>");
    }

    @Override
    public JSONObject generateNgDynamicFormsCls() {
        JSONObject drCls = new JSONObject();

        JSONObject drGrid = new JSONObject();
        drGrid.put("container", "ui-g-12");

        drCls.put("grid", drGrid);
        return drCls;
    }

    @Override
    public Object generateNgDynamicForms(Map<String, Object> inputs) {
        JSONObject dr = new JSONObject();
        
        dr.put("cls", generateNgDynamicFormsCls());

        dr.put("type",  "GROUP");
        dr.put("id",    model.getName());
        dr.put("name",  model.getName());

        //String label = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(model.getName()), " ");
        //dr.put("label", label);

        JSONArray array = myAttributes.generateNgDynamicForms(inputs);

        for (String elementName : subStructureOrder) array.put(subStructure.get(elementName).generateNgDynamicForms(inputs));

        dr.put("group", array);

        return dr;
    }
}
