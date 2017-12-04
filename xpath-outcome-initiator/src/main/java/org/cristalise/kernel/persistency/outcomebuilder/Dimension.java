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

import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ElementDecl;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Dimension extends OutcomeStructure {

    enum Mode {TABLE, TABS};

    DimensionTableModel          tableModel;
    Element                      parent;
    DomKeyPushTable              table;
    ArrayList<DimensionInstance> instances = new ArrayList<DimensionInstance>(); // stores DimensionInstances if tabs
    ArrayList<Element>           elements  = new ArrayList<Element>();           // stores current children
    Mode                         mode;

    public Dimension(ElementDecl model) {
        super(model);

        // decide whether a table or tabs
        try {
            tableModel = new DimensionTableModel(model);
            Logger.msg(8, "Dimension() - name:" + model.getName() + " mode:table");

            mode = Mode.TABLE;

            table = new DomKeyPushTable(tableModel, this);
        }
        catch (OutcomeBuilderException e) {
            // use tabs
            Logger.msg(8, "Dimension() - name:" + model.getName() + " mode:tabs: " + e.getMessage());
            mode = Mode.TABS;
        }
    }

    public void setParentElement(Element parent) {
        this.parent = parent;
    }

    @Override
    public void addInstance(Element myElement, Document parentDoc) throws OutcomeBuilderException {
        Logger.msg(6, "Dimension.addInstance() - adding instance " + (elements.size() + 1) + " for " + myElement.getTagName());

        if (parent == null) setParentElement((Element) myElement.getParentNode());

        // if table, pass to table model
        if (mode == Mode.TABLE) {
            tableModel.addInstance(myElement, -1);
            elements.add(myElement);
        }
        else {
            DimensionInstance target;
            elements.add(myElement);

            if (instances.size() < elements.size()) target = newInstance();
            else                                    target = instances.get(elements.size() - 1);

            target.addInstance(myElement, parentDoc);
        }
    }

    public int getChildCount() {
        return elements.size();
    }

    public DimensionInstance newInstance() {
        DimensionInstance newInstance = null;
        try {
            newInstance = new DimensionInstance(model);
            instances.add(newInstance);
            newInstance.setTabNumber(instances.size());
            newInstance.setParent(this);
        }
        catch (OutcomeBuilderException e) {
            // shouldn't happen, we've already done it once
            Logger.error(e);
        }
        return newInstance;
    }

    @Override
    public String validateStructure() {
        if (mode == Mode.TABLE) {
            return table.validateStructure();
        }
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
        Logger.msg(5, "Dimension.initNew() - '" + model.getName()+"' as '" + mode.name() + "'");

        Element newElement;

        if (mode == Mode.TABLE) {
            newElement = tableModel.initNew(parent, -1);
            elements.add(newElement);
            return newElement;
        }
        else {
            DimensionInstance newTab = null;

            if (instances.size() < elements.size() + 1)  newTab = newInstance();
            else                                         newTab = instances.get(elements.size() - 1);

            newElement = newTab.initNew(parent);
            elements.add(newElement);
            return newElement;
        }
    }

    @Override
    public Element createElement(Document rootDocument, String recordName) { return null; }

    public void addRow(int index) throws OutcomeBuilderException {
        if (elements.size() == model.getMaxOccurs())
            throw new CardinalException("Maximum size of table reached");

        if (mode == Mode.TABLE) {
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

    public void removeRow(int index) throws OutcomeBuilderException {
        if (elements.size() <= model.getMinOccurs())
            throw new CardinalException("Minimum size of table reached");
        if (mode == Mode.TABLE) {
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

    @Override
    public void exportViewTemplate(Writer template) {
        if (mode == Mode.TABLE) {
        }
        else if (mode == Mode.TABS) {
        }
    }

    @Override
    public Object generateNgDynamicForms() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JSONObject generateNgDynamicFormsCls() {
        // TODO Auto-generated method stub
        return null;
    }
}
