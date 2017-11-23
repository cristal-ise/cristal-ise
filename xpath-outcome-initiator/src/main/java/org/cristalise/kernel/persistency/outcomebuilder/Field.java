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

import org.cristalise.kernel.persistency.outcomebuilder.field.StringField;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ElementDecl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class Field extends OutcomeStructure {

    StringField   myFieldInstance = null;
    AttributeList myAttributes;
    Text          textNode;

    public Field(ElementDecl model) {
        super(model);

        try {
            myFieldInstance = StringField.getEditField(model);
            Logger.msg(6, "Field() - name:" + model.getName() + " type: "+myFieldInstance.getClass().getSimpleName());
        }
        catch (StructuralException e) {
            // no base type for field - only attributes
            myFieldInstance = null;
        }

        myAttributes = new AttributeList(model);
    }

    public AttributeList getAttributes() {
        return myAttributes;
    }

    @Override
    public void addStructure(OutcomeStructure newElement) throws OutcomeBuilderException {
        throw new StructuralException("Field "+model.getName()+" cannot have child structures");
    }

    @Override
    public void addInstance(Element myElement, Document parentDoc) throws OutcomeBuilderException {
        Logger.msg(6, "Field.addInstance() - field:"+myElement.getTagName());

        if (this.myElement != null) throw new CardinalException("Field "+this.getName()+" cannot repeat");

        this.myElement = myElement;

        try {
            if (myFieldInstance == null) {
                Logger.error("Field should be empty. Discarding contents.");
            }
            else {
                if (myElement.hasChildNodes())
                    textNode = (Text)myElement.getFirstChild();
                else {
                    textNode = parentDoc.createTextNode(getDefaultValue());
                    myElement.appendChild(textNode);
                }

                myFieldInstance.setData(textNode);
            }
        }
        catch (ClassCastException ex) {
            throw new StructuralException("First child node of Field " + this.getName() + " was not Text: "+myElement.getFirstChild().getNodeType());
        }
        myAttributes.setInstance(myElement);
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
    public Element initNew(Document parent) {
        Logger.msg(6, "Field.initiNew() - Creating '"+this.getName()+"'");

        // make a new Element
        myElement = parent.createElement(this.getName());

        // see if there is a default/fixed value
        if (myFieldInstance != null) {
            // populate
            String defaultVal = getDefaultValue();
            textNode = parent.createTextNode(defaultVal);
            myElement.appendChild(textNode);
            myFieldInstance.setData(textNode);
        }

        // set up attributes
        myAttributes.initNew(myElement);

        return myElement;
    }

    @Override
    public Element createElement(Document rootDocument, String recordName) { return null; }

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
}
