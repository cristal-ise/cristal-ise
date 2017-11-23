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

import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DataRecord extends OutcomeStructure {

    AttributeList myAttributes;
    Document      parentDoc;

    public DataRecord(ElementDecl model)
            throws OutcomeBuilderException
    {
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
    public void addInstance(Element myElement, Document parentDoc) throws OutcomeBuilderException {
        Logger.msg(8, "DataRecord.addInstance() - name:" + myElement.getTagName());

        if (this.myElement != null) throw new CardinalException("DataRecord " + this.getName() + " cannot repeat.");

        this.myElement = myElement;
        this.parentDoc = parentDoc;

        populateInstance();
    }

    public void populateInstance() throws StructuralException, OutcomeBuilderException {
        myAttributes.setInstance(myElement);

        NodeList childElements = myElement.getChildNodes();

        for (int i = 0; i < childElements.getLength(); i++) {
            // ignore chardata here
            if (!(childElements.item(i) instanceof Element)) continue;

            Element thisElement = (Element) childElements.item(i);

            // find the child structure with this name
            OutcomeStructure thisStructure = subStructure.get(thisElement.getTagName());

            if (thisStructure == null)
                throw new StructuralException("DR " + model.getName() + " not expecting " + thisElement.getTagName());

            thisStructure.addInstance(thisElement, parentDoc);
        }

        // make sure any dimensions have the minimum
        for (Object name2 : subStructure.keySet()) {
            String structureName = (String) name2;
            OutcomeStructure thisStructure = subStructure.get(structureName);
            int count = 0;

            if (thisStructure instanceof Dimension) {
                Dimension thisDimension = (Dimension) thisStructure;
                thisDimension.setParentElement(myElement);
                count = thisDimension.getChildCount();
            }
            else
                count = thisStructure.getElement() == null ? 0 : 1;

            int total = thisStructure.getModel().getMinOccurs();

            // if (total == 0) total++;
            for (int i = count; i < total; i++) {
                myElement.appendChild(thisStructure.initNew(parentDoc));
            }
        }
    }

    @Override
    public Element initNew(Document parent) {
        Logger.msg(5, "DataRecord.initNew() - name:'" + model.getName()+"'");

        // make a new Element
        myElement = parent.createElement(model.getName());

        // populate
        for (String elementName : subStructureOrder) {
            OutcomeStructure childStructure = subStructure.get(elementName);

            if (childStructure instanceof Dimension) ((Dimension) childStructure).setParentElement(myElement);

            for (int i = 0; i < childStructure.getModel().getMinOccurs(); i++) {
                myElement.appendChild(childStructure.initNew(parent));
            }
        }

        // set up attributes
        myAttributes.initNew(myElement);

        return myElement;
    }

    @Override
    public Element createElement(Document rootDocument, String recordName) throws OutcomeBuilderException {
        OutcomeStructure childModel = getChildModelElement(recordName);

        if (childModel == null) throw new StructuralException("DR "+model.getName()+"' does not have child '"+recordName+"'");

        Element newElement = childModel.initNew(rootDocument);

        myElement.appendChild(newElement);

        return newElement;
    }
}
