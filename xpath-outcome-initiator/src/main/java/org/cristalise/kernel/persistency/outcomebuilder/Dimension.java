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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ElementDecl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Dimension extends OutcomeStructure {

    DimensionTableModel          tableModel;
    Element                      parent;
    DomKeyPushTable              table;
    ArrayList<DimensionInstance> instances = new ArrayList<DimensionInstance>(); // stores DimensionInstances if tabs
    ArrayList<Element>           elements  = new ArrayList<Element>();           // stores current children
    short                        mode;
    protected static final short TABLE     = 1;
    protected static final short TABS      = 2;

    public Dimension(ElementDecl model, boolean readOnly, HashMap<String, Class<?>> specialControls) {
        super(model, readOnly, specialControls);

        // decide whether a table or tabs
        try {
            tableModel = new DimensionTableModel(model, readOnly);
            Logger.msg(8, "DIM " + model.getName() + " - Will be a table");

            mode = TABLE;

            table = new DomKeyPushTable(tableModel, this);
        }
        catch (StructuralException e) {
            // use tabs
            Logger.msg(8, "DIM " + model.getName() + " - Will be tabs: " + e.getMessage());
            mode = TABS;
        }
    }

    public void setParentElement(Element parent) {
        this.parent = parent;
    }

    @Override
    public void addInstance(Element myElement, Document parentDoc) throws OutcomeException {
        if (Logger.doLog(6))
            Logger.msg(6, "DIM - adding instance " + (elements.size() + 1) + " for " + myElement.getTagName());
        if (parent == null) setParentElement((Element) myElement.getParentNode());
        // if table, pass to table model
        if (mode == TABLE) {
            tableModel.addInstance(myElement, -1);
            elements.add(myElement);
        }
        else {
            DimensionInstance target;
            elements.add(myElement);
            if (instances.size() < elements.size())
                target = newInstance();
            else
                target = instances.get(elements.size() - 1);
            target.addInstance(myElement, parentDoc);
        }
    }

    public int getChildCount() {
        return elements.size();
    }

    public DimensionInstance newInstance() {
        DimensionInstance newInstance = null;
        try {
            newInstance = new DimensionInstance(model, readOnly, deferChild, specialEditFields);
            instances.add(newInstance);
            newInstance.setTabNumber(instances.size());
            newInstance.setParent(this);
            deferChild = true;
        }
        catch (OutcomeException e) {
            // shouldn't happen, we've already done it once
            Logger.error(e);
        }
        return newInstance;
    }

    @Override
    public String validateStructure() {
        if (mode == TABLE)
            return table.validateStructure();
        else {
            StringBuffer errors = new StringBuffer();
            for (Iterator<DimensionInstance> iter = instances.iterator(); iter.hasNext();) {
                OutcomeStructure element = iter.next();
                errors.append(element.validateStructure());
            }
            return errors.toString();
        }
    }

    @Override
    public Element initNew(Document parent) {
        Element newElement;

        if (mode == TABLE) {
            newElement = tableModel.initNew(parent, -1);
            elements.add(newElement);
            return newElement;
        }
        else {
            DimensionInstance newTab = null;
            if (instances.size() < elements.size() + 1)
                newTab = newInstance();
            else
                newTab = instances.get(elements.size() - 1);
            newElement = newTab.initNew(parent);
            elements.add(newElement);
            return newElement;
        }
    }

    public void addRow(int index) throws CardinalException {
        if (elements.size() == model.getMaxOccurs())
            throw new CardinalException("Maximum size of table reached");

        if (mode == TABLE) {
            Element newRow = tableModel.initNew(parent.getOwnerDocument(), index);
            elements.add(index, newRow);
            try {
                Element following = elements.get(index + 1);
                parent.insertBefore(newRow, following);
            }
            catch (IndexOutOfBoundsException ex) {
                parent.appendChild(newRow);
            }

            // FIXME add row to table
            // table.setRowSelectionInterval(index, index);
        }
        else {
            Element newTab = initNew(parent.getOwnerDocument());
            parent.appendChild(newTab);
        }
    }

    public void removeRow(int index) throws CardinalException {
        if (elements.size() <= model.getMinOccurs())
            throw new CardinalException("Minimum size of table reached");
        if (mode == TABLE) {
            parent.removeChild(tableModel.removeRow(index));
            //int selectRow = index;
            //if (index >= tableModel.getRowCount()) selectRow--;
            if (tableModel.getRowCount() > 0) {
                // FIXME remove row from table
                // table.setRowSelectionInterval(selectRow, selectRow);
            }
        }
        else {
            Element elementToGo = elements.get(index);
            parent.removeChild(elementToGo);
            instances.remove(index);
            for (int i = index; i < instances.size(); i++) {
                DimensionInstance thisInstance = instances.get(i);
                thisInstance.setTabNumber(i + 1);
            }
        }
        elements.remove(index);
    }

    private class DomKeyPushTable {
        Dimension dim;

        public DomKeyPushTable(DimensionTableModel model, Dimension parent) {
            this.dim = parent;
        }

        public String validateStructure() {
            return null;
        }
    }
}
