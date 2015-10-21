/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.gui.tabs.outcome.form;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.gui.tabs.outcome.form.field.EditField;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ElementDecl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;



public class Field extends OutcomeStructure {

    EditField myElementPanel = null;
    AttributeList myAttributes;
    JLabel tagName;
    Text textNode;
    boolean fixed;
    public static final JPanel nullPanel = new JPanel();

    public Field(ElementDecl model, boolean readOnly, HashMap<String, Class<?>> specialEditFields) {
        super(model, readOnly, specialEditFields);

        try {
            myElementPanel = EditField.getEditField(model, specialEditFields);
            Logger.msg(6, "Field type: "+myElementPanel.getClass().getName());
            if (readOnly) myElementPanel.setEditable(false);

        } catch (StructuralException e) { // no base type for field - only attributes
            myElementPanel = null;
        }

        myAttributes = new AttributeList(model, readOnly);

        tagName = makeLabel(model.getName(), help);
       
        setupPanel();
    }
    
    private void setupPanel() {
        GridBagLayout gridbag = new java.awt.GridBagLayout();
        setLayout(gridbag);
        GridBagConstraints position = new GridBagConstraints();
        position.anchor = GridBagConstraints.NORTHWEST;
        position.ipadx = 5; position.ipady = 5;
        position.insets = new Insets(5,5,0,0);
        position.gridwidth=1;
        position.gridy=0; position.gridx=0;
        position.weightx=2; position.weighty=0;
        position.fill=GridBagConstraints.NONE;
        gridbag.setConstraints(getLabel(), position);
        this.add(getLabel());
        position.gridy++;
        position.weighty=1;
        position.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(getCData(), position);
        this.add(getCData());
        position.gridx++;
        position.gridy--;
        position.gridheight=2;
        position.weightx=0;
        position.fill=GridBagConstraints.NONE;
        gridbag.setConstraints(getAttributes(), position);
        this.add(getAttributes());
        position.gridx=0;
        position.gridheight=1;
        position.gridy++;
    }

    public JComponent getLabel() {
        return tagName;
    }

    public Component getCData() {
        if (myElementPanel == null)
            return nullPanel;
        return myElementPanel.getControl();
    }

    public JComponent getAttributes() {
        return myAttributes;
    }

    @Override
	public void addStructure(OutcomeStructure newElement) throws StructuralException {
        throw new StructuralException("Field "+model.getName()+" cannot have child structures");
    }

    @Override
	public void addInstance(Element myElement, Document parentDoc) throws OutcomeException {
        Logger.msg(6, "Accepting Field "+myElement.getTagName());
        if (this.myElement != null) throw new CardinalException("Field "+this.getName()+" cannot repeat");
        this.myElement = myElement;

        try {
            if (myElementPanel == null)
                Logger.error("Field should be empty. Discarding contents.");
            else {
                if (myElement.hasChildNodes())
                    textNode = (Text)myElement.getFirstChild();
                else {
                    textNode = parentDoc.createTextNode(getDefaultValue());
                    myElement.appendChild(textNode);
                }

                myElementPanel.setData(textNode);
            }
        } catch (ClassCastException ex) {
            throw new StructuralException("First child node of Field " + this.getName() + " was not Text: "+myElement.getFirstChild().getNodeType());
        }
        myAttributes.setInstance(myElement);
    }

        // check if valid

    @Override
	public String validateStructure() {
        myAttributes.validateAttributes();
        if (myElementPanel != null) myElementPanel.updateNode();
        Text contents = (Text)myElement.getFirstChild();
        if (!myElement.hasAttributes() && model.getMinOccurs() < 1 &&
            (contents == null || contents.getData().length() == 0))
            // empty - should remove if optional
            myElement.getParentNode().removeChild(myElement);
        return null;
    }

    @Override
	public Element initNew(Document parent) {
        Logger.msg(6, "Creating Field "+this.getName());

        // make a new Element
        myElement = parent.createElement(this.getName());

        // see if there is a default/fixed value
        if (myElementPanel != null) {
            // populate
            String defaultVal = readOnly?"":getDefaultValue();
            textNode = parent.createTextNode(defaultVal);
            myElement.appendChild(textNode);
            myElementPanel.setData(textNode);
        }

        // set up attributes
        myAttributes.initNew(myElement);

        return myElement;
    }

    private String getDefaultValue() {
        String defaultValue = model.getFixedValue();
        if (defaultValue == null) defaultValue = model.getDefaultValue();
        if (defaultValue == null) defaultValue = myElementPanel.getDefaultValue();
        return defaultValue;
    }

    @Override
	public void grabFocus() {
        if (myElementPanel != null)
            myElementPanel.grabFocus();
        else
            myAttributes.grabFocus();
    }

}
