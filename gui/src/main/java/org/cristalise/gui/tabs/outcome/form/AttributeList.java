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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.cristalise.gui.tabs.outcome.form.field.EditField;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;


public class AttributeList extends JPanel {

    ArrayList<EditField> attrSet = new ArrayList<EditField>();
    ElementDecl model;
    Element myElement;
    boolean readOnly;
    static Font labelFont;

    public AttributeList(ElementDecl model, boolean readOnly) {
        super();
        AttributeDecl thisDecl;
        this.model = model;
        this.readOnly = readOnly;

        // set up panel
        GridBagLayout gridbag = new java.awt.GridBagLayout();
        setLayout(gridbag);
        if (labelFont == null)
            labelFont = this.getFont().deriveFont((float)(this.getFont().getSize()-3.0));
        // retrieve attributes
        if (!model.getType().isComplexType()) {
                // simple types have no attributes
            return;
        }

        ComplexType content = (ComplexType)model.getType();

        // place on panel

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0; c.weighty = 1.0; c.gridx = 0;
        c.ipadx = 5; c.ipady = 0;

        for (Enumeration<?> fields = content.getAttributeDecls(); fields.hasMoreElements();) {
            c.gridy = 0;
            thisDecl = (AttributeDecl)fields.nextElement();
            Logger.msg(8, "Includes Attribute "+thisDecl.getName());

            // read help
            String helpText = null;
            String doc = OutcomeStructure.extractHelp(thisDecl);
            if (doc.length() > 0)
                helpText = doc.toString();
            
            // Add Label
            
            JLabel heading = OutcomeStructure.makeLabel(thisDecl.getName(), helpText);
            gridbag.setConstraints(heading, c);
            this.add(heading);

            c.gridy++;

            // Add entry
            try {
                EditField entry = EditField.getEditField(thisDecl);
                attrSet.add(entry);
                if (readOnly) entry.setEditable(false);
                gridbag.setConstraints(entry.getControl(), c);
                this.add(entry.getControl());
            } catch (StructuralException e) {
                JLabel entry = new JLabel("Error");
                entry.setToolTipText(e.getMessage());
                gridbag.setConstraints(entry, c);
                this.add(entry);
            }


            c.gridx++;
        }
    }

    public void setInstance(Element data) throws StructuralException {
        this.myElement = data;
        for (EditField thisField : attrSet) {
            Logger.msg(8, "Populating Attribute "+thisField.getName());
            Attr thisAttr = myElement.getAttributeNode(thisField.getName());
            if (thisAttr == null)
                thisAttr = newAttribute(myElement, (AttributeDecl)thisField.getModel());
            thisField.setData(thisAttr);
        }
    }

    public Attr newAttribute(Element parent, AttributeDecl attr) {

        parent.setAttribute(attr.getName(), attr.getFixedValue()!=null?attr.getFixedValue():attr.getDefaultValue());
        return parent.getAttributeNode(attr.getName());
    }

    public String validateAttributes() {
        if (model.getType().isComplexType()) {
            ComplexType content = (ComplexType)model.getType();
            for (Enumeration<?> fields = content.getAttributeDecls(); fields.hasMoreElements();) {
                AttributeDecl thisDecl = (AttributeDecl)fields.nextElement();
                String attrVal = myElement.getAttribute(thisDecl.getName());
                if (attrVal.length() == 0 && thisDecl.isOptional()) {
                    myElement.removeAttribute(thisDecl.getName());
                }
            }
        }
        return null;
    }

    public void initNew(Element parent) {
        AttributeDecl thisDecl;
        EditField thisField;
        Attr thisAttr;
        this.myElement = parent;

        if (model.getType().isSimpleType()) return; // no attributes in simple types

        ComplexType content = (ComplexType)model.getType();

        for (Iterator<EditField> e = attrSet.iterator(); e.hasNext();) {
            thisField = e.next();

            thisDecl = content.getAttributeDecl(thisField.getName());
            // HACK: if we don't resolve the reference, the type will be null
            if (thisDecl.isReference()) thisDecl = thisDecl.getReference();
            thisAttr = newAttribute(myElement, thisDecl);
            // add into parent - fill in field
            try {
                thisField.setData(thisAttr);
            } catch (Exception ex) { } // impossible name mismatch
        }
    }
    @Override
	public void grabFocus() {
        if (attrSet.size() > 0)
            attrSet.get(0).grabFocus();
    }
}
