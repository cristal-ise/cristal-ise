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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.table.JTableHeader;

import org.cristalise.gui.DomainKeyConsumer;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Particle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class Dimension extends OutcomeStructure implements ActionListener {

    DimensionTableModel tableModel;
    Element parent;
    GridBagConstraints position;
    GridBagLayout gridbag;
    JTabbedPane tabs;
    JLabel msg;
    DomKeyPushTable table;
    Box tableBox;
    ArrayList<DimensionInstance> instances = new ArrayList<DimensionInstance>(); // stores DimensionInstances if tabs
    ArrayList<Element> elements = new ArrayList<Element>(); // stores current children

    JButton addButton;
    JButton delButton;

    short mode;
    protected static final short TABLE = 1;
    protected static final short TABS = 2;


    public Dimension(ElementDecl model, boolean readOnly, HashMap<String, Class<?>> specialControls) {
        super(model, readOnly, specialControls);
        // set up panel
        gridbag = new java.awt.GridBagLayout();
        setLayout(gridbag);
        position = new GridBagConstraints();
        position.anchor = GridBagConstraints.NORTHWEST;
        position.fill = GridBagConstraints.HORIZONTAL;
        position.weightx = 1.0; position.weighty = 0.0;
        position.gridx = 0; position.gridy = 0;
        position.ipadx = 0; position.ipady = 0;
        position.insets = new Insets(0,0,0,0);

        // set up the border
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), model.getName()));

        msg = new JLabel("No elements");
        msg.setFont(new Font("SansSerif", Font.ITALIC, msg.getFont().getSize()));
        gridbag.setConstraints(msg, position);
        add(msg);
        position.gridy++;

        // decide whether a table or tabs
        try {
            tableModel = new DimensionTableModel(model, readOnly);
            Logger.msg(8, "DIM "+model.getName()+" - Will be a table");
            if (help != null) {
            	add(makeLabel(null, help), position);
            	position.gridy++;
            }
            mode = TABLE;
            tableBox = Box.createVerticalBox();
            // help icon if needed
            table = new DomKeyPushTable(tableModel, this);
            new MultiLinePasteAdapter(table, this);
            if (readOnly) table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            table.setColumnSelectionAllowed(readOnly);
            JTableHeader tableHeader = table.getTableHeader();
            tableHeader.setReorderingAllowed(false);
            tableBox.add(tableHeader);
            tableBox.add(table);
            gridbag.setConstraints(tableBox, position);
            add(tableBox);
            tableBox.setVisible(false);

        } catch (StructuralException e) {
            // use tabs
            Logger.msg(8, "DIM "+model.getName()+" - Will be tabs: "+e.getMessage());
            mode = TABS;
            tabs = new JTabbedPane();
            gridbag.setConstraints(tabs, position);
            add(tabs);
            tabs.setVisible(false);
        }
        if (!readOnly) {
            JPanel rowAdjust = new JPanel(new FlowLayout());
            addButton = new JButton("+");
            addButton.setActionCommand("add");
            addButton.addActionListener(this);
            rowAdjust.add(addButton);

            delButton = new JButton("-");
            delButton.setActionCommand("del");
            delButton.addActionListener(this);
            delButton.setEnabled(false);
            rowAdjust.add(delButton);


            position.gridy++; position.weighty=0; position.weightx=0;
            gridbag.setConstraints(rowAdjust, position);
            this.add(rowAdjust);
            }

    }

    public void setParentElement(Element parent) {
        this.parent = parent;
    }

    @Override
	public void addInstance(Element myElement, Document parentDoc) throws OutcomeException {
        if (Logger.doLog(6))
            Logger.msg(6, "DIM - adding instance "+ (elements.size()+1) +" for "+myElement.getTagName());
        if (parent == null) setParentElement((Element)myElement.getParentNode());
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
                target = instances.get(elements.size()-1);
            target.addInstance(myElement, parentDoc);
            tabs.setTitleAt(tabs.indexOfComponent(target), target.getName());
        }
        checkButtons();
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
            tabs.addTab(newInstance.getName(), newInstance);
            tabs.addChangeListener(newInstance);
        } catch (OutcomeException e) {
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

    public void checkButtons() {
        // check if data visible
        boolean dataVisible = elements.size() > 0;
        if (mode == TABS) tabs.setVisible(dataVisible);
        else tableBox.setVisible(dataVisible);
        msg.setVisible(!dataVisible);

        if (readOnly) return;

        if (elements.size() <= model.getMinOccurs() || elements.size() == 0) {
            delButton.setEnabled(false);
            delButton.setToolTipText("Minimum row count of "+model.getMinOccurs()+" reached.");
        } else {
            delButton.setEnabled(true);
            delButton.setToolTipText(null);
        }

        if (elements.size() < model.getMaxOccurs() || model.getMaxOccurs() == Particle.UNBOUNDED) {
            addButton.setEnabled(true);
            addButton.setToolTipText(null);
        } else {
             addButton.setEnabled(false);
             addButton.setToolTipText("Maximum row count of "+model.getMaxOccurs()+" reached.");
        }
    }

    @Override
	public Element initNew(Document parent) {
        Element newElement;

        if (mode == TABLE) {
            newElement = tableModel.initNew(parent, -1);
            elements.add(newElement);
            checkButtons();
            return newElement;
        }
        else  {
            DimensionInstance newTab = null;
            if (instances.size() < elements.size()+1)
                newTab = newInstance();
            else
                newTab = instances.get(elements.size()-1);
            newElement = newTab.initNew(parent);
            elements.add(newElement);
            checkButtons();
            return newElement;
        }
    }

    @Override
	public void actionPerformed(ActionEvent e) {
        int index;
        if (mode == TABS) index = tabs.getSelectedIndex();
        else {
            index = table.getSelectedRow();
            if (index == -1) index = tableModel.getRowCount();
        }
        try {
        	if (table == null || table.getCellEditor() == null || table.getCellEditor().stopCellEditing()) {
	            if (e.getActionCommand().equals("add"))
	                addRow(index);
	            else if (e.getActionCommand().equals("del"))
	                removeRow(index);
	        }
        } catch (CardinalException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Table error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void addRow(int index) throws CardinalException {
        if (elements.size() == model.getMaxOccurs())
            throw new CardinalException("Maximum size of table reached");

        if (mode == TABLE) {
            Element newRow = tableModel.initNew(parent.getOwnerDocument(), index);
            elements.add(index, newRow);
            try {
                Element following = elements.get(index+1);
                parent.insertBefore(newRow, following);
            } catch (IndexOutOfBoundsException ex) {
                parent.appendChild(newRow);
            }
            table.clearSelection();
            table.setRowSelectionInterval(index, index);
        }
        else {
            Element newTab = initNew(parent.getOwnerDocument());
            parent.appendChild(newTab);
        }
        checkButtons();

    }

    public void removeRow(int index) throws CardinalException {
        if (elements.size() <= model.getMinOccurs())
            throw new CardinalException("Minimum size of table reached");
        if (mode == TABLE) {
            parent.removeChild(tableModel.removeRow(index));
            int selectRow = index;
            if (index >= tableModel.getRowCount()) selectRow--;
            if (tableModel.getRowCount() > 0) {
                table.clearSelection();
                table.setRowSelectionInterval(selectRow, selectRow);
            }
        }
        else {
            Element elementToGo = elements.get(index);
            parent.removeChild(elementToGo);
            instances.remove(index);
            tabs.remove(index);
            for (int i = index; i<instances.size(); i++) {
                DimensionInstance thisInstance = instances.get(i);
                thisInstance.setTabNumber(i+1);
                tabs.setTitleAt(i, thisInstance.getName());
            }
        }
        elements.remove(index);
        checkButtons();
    }

    private class DomKeyPushTable extends JTable implements DomainKeyConsumer, FocusListener {

        Dimension dim;
        public DomKeyPushTable(DimensionTableModel model, Dimension parent) {
            super(model);
            addFocusListener(this);
            putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            this.dim = parent;
        }

        @Override
		public void push(DomainPath key) {
            push(key.getName());
        }

        @Override
		public void push(String name) {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (cellEditor != null)
                cellEditor.stopCellEditing();
            Logger.msg(8, "Pushing "+name+" to table at "+row+","+col);
            if (col > -1 && row > -1) {
                if (dataModel.getValueAt(row, col).toString().length()==0)
                    dataModel.setValueAt(name, row, col);
                else {
                    if (row+1 == getRowCount()) {
                        try {
                            dim.addRow(row+1);
                            dataModel.setValueAt(name, row+1, col);
                        } catch (CardinalException ex) {
                            JOptionPane.showMessageDialog(null, ex.getMessage(), "Table error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                if (row+1 < getRowCount()) {
                    Logger.msg(8, "Shifting selection to row "+(row+1));
                    changeSelection(row+1, col, false, false);
                }
            }
        }

        @Override
		public void focusGained(FocusEvent e) {
            if (!readOnly)
                MainFrame.itemFinder.setConsumer(this, "Insert");
        }

        @Override
		public void focusLost(FocusEvent e) {
            // release the itemFinder
            if (!readOnly)
                MainFrame.itemFinder.clearConsumer(this);
        }

        public String validateStructure() {
            if (cellEditor != null)
                cellEditor.stopCellEditing();
            return null;
        }

    }

    @Override
	public void grabFocus() {
        if (mode == TABLE) {
            if (table.getSelectedRow() == -1 && table.getRowCount() > 0) {
                table.changeSelection(0, 0, false, false);
                table.editCellAt(0,0);
            }
            table.requestFocus();
        }
        else if (instances.size()> 0)
            instances.get(0).grabFocus();
    }

}
