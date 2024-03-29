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
import java.util.Map;
import java.util.Map.Entry;

import org.exolab.castor.xml.schema.ElementDecl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Dimension extends OutcomeStructure {

    enum Mode {TABLE, TABS};

    DimensionTableModel          tableModel;
    Element                      parent;
//    DomKeyPushTable              table;
    ArrayList<DimensionInstance> instances = new ArrayList<DimensionInstance>(); // stores DimensionInstances if tabs
    ArrayList<Element>           elements  = new ArrayList<Element>();           // stores current children
    Mode                         mode;

    public Dimension(ElementDecl model) {
        super(model);

        log.debug("ctor() - name:{} optional:{} isAnyType:{}", model.getName(), isOptional(), isAnyType());

        // decide whether a table or tabs
        try {
            tableModel = new DimensionTableModel(model);
            log.debug("name:" + model.getName() + " mode:table");

            mode = Mode.TABLE;

//            table = new DomKeyPushTable(tableModel, this);
        }
        catch (OutcomeBuilderException e) {
            // use tabs
            log.debug("name:" + model.getName() + " mode:tabs  ex:" + e.getMessage());
            mode = Mode.TABS;
        }
    }

    public void setParentElement(Element parent) {
        this.parent = parent;
    }

    @Override
    public void addInstance(Element newElement, Document parentDoc) throws OutcomeBuilderException {
        log.debug("addInstance() - adding instance " + (elements.size() + 1) + " for " + newElement.getTagName());

        if (parent == null) setParentElement((Element) newElement.getParentNode());

        // if table, pass to table model
        if (mode == Mode.TABLE) {
            tableModel.addInstance(newElement, -1);
            elements.add(newElement);
        }
        else {
            DimensionInstance target;
            elements.add(newElement);

            if (instances.size() < elements.size()) target = newInstance();
            else                                    target = instances.get(elements.size() - 1);

            target.addInstance(newElement, parentDoc);
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
            log.error("", e);
        }
        return newInstance;
    }

    @Override
    public String validateStructure() {
        if (mode == Mode.TABLE) {
//            return table.validateStructure();
            return "";
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
        log.debug("initNew() - '" + model.getName()+"' as '" + mode.name() + "'");
        
        Element newElement;

        if (mode == Mode.TABLE) {
            newElement = tableModel.initNew(parent, -1);
            elements.add(newElement);
        }
        else {
            DimensionInstance newTab = null;

            if (instances.size() < elements.size() + 1)  newTab = newInstance();
            else                                         newTab = instances.get(elements.size() - 1);

            newElement = newTab.initNew(parent);
            elements.add(newElement);
        }

        myElement = newElement;

        return newElement;
    }
/*
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
*/
    @Override
    public void exportViewTemplate(Writer template) {
        if (mode == Mode.TABLE) {
        }
        else if (mode == Mode.TABS) {
        }
    }

    @Override
    public Object generateNgDynamicForms(Map<String, Object> inputs, boolean withModel, boolean withLayout) {
        if (mode == Mode.TABLE && withModel) {
            JSONObject table = new JSONObject();
            table.put("type",  "TABLE");
            table.put("id",    model.getName());
            table.put("name",  model.getName());

            JSONArray columns = new JSONArray();
            table.put("columns",  columns);
            for (Entry<String, Field> entry: tableModel.columns.entrySet()) {
                columns.put(entry.getValue().generateNgDynamicForms(inputs, withModel, withLayout));
            }

            JSONArray rows = new JSONArray();
            table.put("rows",  rows);

            return table;
        }
        return null;
    }

    @Override
    public JSONObject generateNgDynamicFormsCls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addJsonInstance(OutcomeStructure parentStruct, Element parentElement, String name, Object json) throws OutcomeBuilderException {
        log.debug("addJsonInstance() - name:'" + name + "', mode:"+mode);

        if (myElement == null) myElement = parent;

        if (!name.equals(model.getName())) throw new InvalidOutcomeException("Missmatch in names:" + name + "!=" + model.getName());

        if (mode == Mode.TABLE) {
            JSONArray jsonArray = (JSONArray)json;
            int i = 0;
            for (Object element : jsonArray) {
                JSONObject jsonObj = (JSONObject)element;

                if (tableModel.getRowCount() < i+1) {
                    Element newElement = tableModel.initNew(parent.getOwnerDocument(), i);
                    parentStruct.addChildElement(name, newElement);
                    elements.add(newElement);
                }

                for (String key: jsonObj.keySet()) {
                    Object value = jsonObj.get(key);
                    tableModel.setValueAt(value, i, key);
                }

                i++;
            }
        }
        else
            throw new UnsupportedOperationException("Dimension cannot process TABS yet");
    }

    /**
     * Dimension cannot use the subStructure inherited from OutcomeStructure
     */
    @Override
    public OutcomeStructure getChildModelElement(String name) {
        if (mode == Mode.TABLE) {
            return tableModel.columns.get(name);
        }
        else {
            log.warn("getChildModelElement("+model.getName()+") - Does not handle TAB mode for child:"+name);
            return null;
        }
    }

    /**
     * Adds the child element at the correct position using the expected sequence of elements (tableModel.columnHeadings)
     * Dimension cannot use the subStructureOrder inherited from OutcomeStructure
     */
    @Override
    public void addChildElement(String name, Element newElement) {
        if (mode == Mode.TABS) {
            log.warn("addChildElement("+model.getName()+") - Does not handle TAB mode for child:"+name);
            return;
        }

        Element refElement = null;
        boolean cont = true;

        // lets find out where to insert this new element
        for (int i = 0; i < tableModel.columnHeadings.size()-1 && cont; i++) {
            if (name.equals(tableModel.columnHeadings.get(i))) {
                cont = false;

                for (int k = i+1; k < tableModel.columnHeadings.size() && refElement == null; k++) {

                    String refElementName = tableModel.columnHeadings.get(k);
                    NodeList children = myElement.getChildNodes();

                    for (int j = 0; j < children.getLength() && refElement == null; j++) {
                        Node child = children.item(j);
                        // ignore any Node (e.g. Text) which are not Element type
                        if (child instanceof Element && child.getNodeName().equals(refElementName)) {
                            refElement = (Element) child;
                        }
                    }
                }
            }
        }

        if (refElement == null) myElement.appendChild(newElement);
        else                    myElement.insertBefore(newElement, refElement);
    }
}
