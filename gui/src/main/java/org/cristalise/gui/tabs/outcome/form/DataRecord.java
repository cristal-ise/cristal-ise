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
package org.cristalise.gui.tabs.outcome.form;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DataRecord extends OutcomeStructure implements ChangeListener {

    AttributeList myAttributes;
    JTabbedPane childDRTabs = null;
    boolean deferred;
    Document parentDoc;
    GridBagConstraints position;
    GridBagLayout gridbag;

    public DataRecord(ElementDecl model, boolean readOnly, boolean deferred, HashMap<String, Class<?>> specialEditFields) throws OutcomeException {
        super(model, readOnly, specialEditFields);
        this.deferred = deferred;
        if (!deferred) setupPanel();
    }

    public synchronized void activate() {
        deferred = false;
        try {
            setupPanel();
            if (myElement!=null) populateInstance();
        } catch (OutcomeException ex) {
            removeAll();
            setLayout(new FlowLayout());
            add(new JLabel("Error displaying outcome segment: "+ex.getMessage()));
        }
        validate();
    }

    private void setupPanel() throws OutcomeException {
        // set up panel
        gridbag = new java.awt.GridBagLayout();
        setLayout(gridbag);
        position = new GridBagConstraints();
        position.anchor = GridBagConstraints.NORTHWEST;
        position.fill = GridBagConstraints.NONE;
        position.weightx = 1.0; position.weighty = 1.0;
        position.gridx = 0; position.gridy = 0;
        position.ipadx = 5; position.ipady = 5;
        position.insets = new Insets(5,5,0,0);
        
        // help icon if needed
        if (help != null) {
        	position.fill = GridBagConstraints.BOTH;
        	add(makeLabel(null, help), position);
        	position.gridy++;
        }

        // attributes at the top
        myAttributes = new AttributeList(model, readOnly);
        position.gridwidth=3;
        gridbag.setConstraints(myAttributes, position);
        add(myAttributes);

        ComplexType elementType;
        try {
            elementType = (ComplexType)model.getType();
        }
        catch (ClassCastException e) {
            throw new StructuralException("DataRecord created with non-ComplexType");
        }

        // loop through all schema sub-elements
        try {
            enumerateElements(elementType);
        } catch (OutcomeException e) {
            throw new OutcomeException("Element "+model.getName()+" could not be created: "+e.getMessage());
        }
    }

    @Override
	public void addStructure(OutcomeStructure newElement) throws OutcomeException {
        super.addStructure(newElement);
        if (newElement == null) return;
        if (newElement instanceof DataRecord) {
            DataRecord newRecord = (DataRecord)newElement;
            // set up enclosing tabbed pane for child drs
            if (childDRTabs == null) {
                childDRTabs = new JTabbedPane();
                position.gridy++;
                position.weightx=1.0;
                position.fill=GridBagConstraints.HORIZONTAL;
                position.gridwidth=3;
                gridbag.setConstraints(childDRTabs, position);
                add(childDRTabs);
                // defer further tabs in this pane
                deferChild = true;
            }
            childDRTabs.addTab(newRecord.getName(), newRecord);
            childDRTabs.addChangeListener(newRecord);
        }
        else {
            childDRTabs = null;// have to make a new tabbed pane now
            deferChild = false;
            position.fill=GridBagConstraints.HORIZONTAL;
            position.gridwidth=3;
            position.weightx=1.0;
            position.gridy++;
            position.weighty=1.0;
            gridbag.setConstraints(newElement, position);
            add(newElement);
        }
    }

    @Override
	public void addInstance(Element myElement, Document parentDoc) throws OutcomeException {
        if (this.myElement != null) throw new CardinalException("DataRecord "+this.getName()+" cannot repeat.");
        this.myElement = myElement;
        this.parentDoc = parentDoc;
        if (!deferred)
            populateInstance();
    }

    public void populateInstance() throws OutcomeException {
        myAttributes.setInstance(myElement);

        NodeList childElements = myElement.getChildNodes();

        for (int i=0; i<childElements.getLength();i++) {
            if (!(childElements.item(i) instanceof Element)) // ignore chardata here
                continue;
            Element thisElement = (Element) childElements.item(i);

            // find the child structure with this name
            OutcomeStructure thisStructure = subStructure.get(thisElement.getTagName());
            if (thisStructure == null)
                throw new StructuralException("DR "+model.getName()+" not expecting "+thisElement.getTagName());
            thisStructure.addInstance(thisElement, parentDoc);
        }

        // make sure any dimensions have the minimum
        for (Object name2 : subStructure.keySet()) {
            String structureName = (String)name2;
            OutcomeStructure thisStructure = subStructure.get(structureName);
            int count = 0;

            if (thisStructure instanceof Dimension) {
                Dimension thisDimension = (Dimension)thisStructure;
                thisDimension.setParentElement(myElement);
                count = thisDimension.getChildCount();
            }
            else
                count = thisStructure.getElement()==null?0:1;

            int total = thisStructure.getModel().getMinOccurs();
            //if (total == 0) total++;
            for (int i = count;i<total;i++) {
                myElement.appendChild(thisStructure.initNew(parentDoc));
            }
        }
    }

    @Override
	public Element initNew(Document parent) {
        if (deferred) activate();

        // make a new Element
        myElement = parent.createElement(model.getName());
        // populate
        for (Object name2 : order) {
            String structureName = (String)name2;
            OutcomeStructure thisStructure = subStructure.get(structureName);
            if (thisStructure instanceof Dimension)
                ((Dimension)thisStructure).setParentElement(myElement);
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

    @Override
	public void stateChanged(ChangeEvent e) {
        JTabbedPane targetPane = (JTabbedPane)e.getSource();
        DataRecord targetTab = (DataRecord)targetPane.getSelectedComponent();
        if (targetTab == this) {
            if (deferred) SwingUtilities.invokeLater(
                    new Thread(new Runnable() {
                        @Override
						public void run() {
                            activate();
                            }
                        }
                    ));
        }
    }

    /**
     * sets focus to first editable child
     */
    @Override
	public void grabFocus() {
        if (myAttributes.attrSet.size() > 0)
            myAttributes.grabFocus();
        else if (order.size()> 0)
            subStructure.get(order.get(0)).grabFocus();
    }
}
