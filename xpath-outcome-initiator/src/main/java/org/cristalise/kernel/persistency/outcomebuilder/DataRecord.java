/**
 * This file is part of the CRISTAL-iSE default user interface.
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

import java.util.HashMap;

import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DataRecord extends OutcomeStructure {

    AttributeList myAttributes;
    boolean       deferred;
    Document      parentDoc;

    public DataRecord(ElementDecl model, boolean readOnly, boolean deferred, HashMap<String, Class<?>> specialEditFields)
            throws OutcomeException
    {
        super(model, readOnly, specialEditFields);
        this.deferred = deferred;
        if (!deferred) setupPanel();
    }

    public synchronized void activate() {
        deferred = false;
        try {
            setupPanel();
            if (myElement != null) populateInstance();
        }
        catch (OutcomeException ex) {
            Logger.error(ex);
        }
    }

    private void setupPanel() throws OutcomeException {
        // set up panel
        // attributes at the top
        myAttributes = new AttributeList(model, readOnly);

        ComplexType elementType;
        try {
            elementType = (ComplexType) model.getType();
        }
        catch (ClassCastException e) {
            throw new StructuralException("DataRecord created with non-ComplexType");
        }

        // loop through all schema sub-elements
        try {
            enumerateElements(elementType);
        }
        catch (OutcomeException e) {
            throw new OutcomeException("Element " + model.getName() + " could not be created: " + e.getMessage());
        }
    }

    @Override
    public void addStructure(OutcomeStructure newElement) throws OutcomeException {
        super.addStructure(newElement);
        if (newElement == null) return;

        //FIXME: perhaps this is just a leftover from the GUI code
        if (newElement instanceof DataRecord) {
            //DataRecord newRecord = (DataRecord) newElement;
            deferChild = true;
        }
        else {
            deferChild = false;
        }
    }

    @Override
    public void addInstance(Element myElement, Document parentDoc) throws OutcomeException {
        Logger.msg(8, "Accepting DR " + myElement.getTagName());
        if (this.myElement != null) throw new CardinalException("DataRecord " + this.getName() + " cannot repeat.");
        this.myElement = myElement;
        this.parentDoc = parentDoc;

        if (!deferred)
            populateInstance();
    }

    public void populateInstance() throws OutcomeException {
        myAttributes.setInstance(myElement);

        NodeList childElements = myElement.getChildNodes();

        for (int i = 0; i < childElements.getLength(); i++) {
            if (!(childElements.item(i) instanceof Element)) // ignore chardata here
                continue;
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
        Logger.msg(6, "Creating DR " + model.getName());
        if (deferred) activate();

        // make a new Element
        myElement = parent.createElement(model.getName());
        // populate
        for (Object name2 : order) {
            String structureName = (String) name2;

            OutcomeStructure thisStructure = subStructure.get(structureName);

            if (thisStructure instanceof Dimension) ((Dimension) thisStructure).setParentElement(myElement);

            int count = 0;
            while (count < thisStructure.getModel().getMinOccurs()) {
                myElement.appendChild(thisStructure.initNew(parent));
                count++;
            }
        }

        // set up attributes
        myAttributes.initNew(myElement);

        return myElement;
    }
}
