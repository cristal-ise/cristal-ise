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
package org.cristalise.gui.tabs.collection;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.graph.view.PropertyTable;
import org.cristalise.gui.graph.view.PropertyTableModel;
import org.cristalise.gui.tabs.ItemTabPane;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.Language;


public class CollectionMemberPropertyPanel extends JPanel implements ActionListener {

    private final PropertyTableModel mPropertyModel;
    private final PropertyTable mPropertyTable;
    private boolean isEditable = false;
    CollectionMember selectedCollMem;
    GridBagLayout gridbag = new GridBagLayout();
    protected JLabel selObjSlot;
    protected JLabel selObjMember;
    JButton addPropButton;
    JButton delPropButton;
    Box newPropBox;
    private JTextField newPropName;
    private JComboBox newPropType;
    String[] typeOptions = { "String", "Boolean", "Integer", "Float" };
    String[] typeInitVal = { "", "false", "0", "0.0"};

	public CollectionMemberPropertyPanel() {
        super();
        setLayout(gridbag);
        mPropertyModel = new PropertyTableModel();
        mPropertyTable = new PropertyTable(mPropertyModel);
        createLayout();
	}

	public void setMember(CollectionMember cm) {
		selectedCollMem = cm;
		String newMemberName;
		try {
			ItemPath path = cm.getItemPath();
			if (path == null) newMemberName = "No member";
			else newMemberName = Gateway.getProxyManager().getProxy(path).getProperty("Name");
		} catch (ObjectNotFoundException e) {
			newMemberName = "Item or Item name property not found";
		}
		
		selObjSlot.setText(newMemberName);
        selObjMember.setText("Slot "+cm.getID());
        mPropertyModel.setMap(cm.getProperties());
        addPropButton.setEnabled(isEditable);
        delPropButton.setEnabled(isEditable);
	}
	
    public void createLayout()
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.ipadx = 5;
        c.ipady = 5;

        selObjSlot = new JLabel();
        selObjSlot.setFont(ItemTabPane.titleFont);
        gridbag.setConstraints(selObjSlot, c);
        add(selObjSlot);

        c.gridy++;
        selObjMember = new JLabel();
        gridbag.setConstraints(selObjMember, c);
        add(selObjMember);

        c.gridy++;
        JLabel title = new JLabel("Properties");
        title.setFont(ItemTabPane.titleFont);
        gridbag.setConstraints(title, c);
        add(title);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 2;
        JScrollPane scroll = new JScrollPane(mPropertyTable);
        gridbag.setConstraints(scroll, c);
        add(scroll);

        newPropBox = Box.createHorizontalBox();
        newPropBox.add(new JLabel(Language.translate("New :")));
        newPropBox.add(Box.createHorizontalGlue());
        newPropName = new JTextField(15);
        newPropBox.add(newPropName);
        newPropType = new JComboBox(typeOptions);
        newPropBox.add(newPropType);
        newPropBox.add(Box.createHorizontalStrut(1));
        addPropButton = new JButton("Add");
        addPropButton.setMargin(new Insets(0, 0, 0, 0));
        delPropButton = new JButton("Del");
        delPropButton.setMargin(new Insets(0, 0, 0, 0));
        addPropButton.addActionListener(this);
        delPropButton.addActionListener(this);
        newPropBox.add(addPropButton);
        newPropBox.add(delPropButton);

        c.gridy++;
        c.weighty=0;
        c.fill= GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(newPropBox, c);
        add(newPropBox);
    }
    
    public void clear() {
        selObjSlot.setText("");
        selObjMember.setText("Nothing Selected");
        mPropertyModel.setMap(new CastorHashMap());
        addPropButton.setEnabled(false);
        delPropButton.setEnabled(false);
    }

    /**
     * @param isEditable The isEditable to set.
     */
    public void setEditable(boolean editable) {
        mPropertyModel.setEditable(editable);
        isEditable = editable;
        newPropBox.setVisible(editable);
    }
    
    @Override
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addPropButton) {
            if (newPropName.getText().length() < 1) {
                JOptionPane.showMessageDialog(this, "Enter a name for the new property", "Cannot add property", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (mPropertyModel.sourceMap.containsKey(newPropName.getText())) {
                JOptionPane.showMessageDialog(this, "Property '"+newPropName.getText()+"' already exists.", "Cannot add property", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (mPropertyTable.getCellEditor() != null)
            	mPropertyTable.getCellEditor().stopCellEditing();

            try {
                Class<?> newPropClass = Class.forName("java.lang."+typeOptions[newPropType.getSelectedIndex()]);
                Class<?>[] params = {String.class};
                Constructor<?> init = newPropClass.getConstructor(params);
                Object[] initParams = { typeInitVal[newPropType.getSelectedIndex()] };
                mPropertyModel.addProperty(newPropName.getText(), init.newInstance(initParams), false);
            } catch (Exception ex) {
            	MainFrame.exceptionDialog(ex);
            }
        }
        else if (e.getSource() == delPropButton) {
            int selrow = mPropertyTable.getSelectedRow();
            if (selrow == -1) {
                JOptionPane.showMessageDialog(this, "Select a property to remove", "Cannot delete property", JOptionPane.ERROR_MESSAGE);
                return;
            }
            mPropertyModel.delProperty(mPropertyModel.sortedNameList.get(selrow));
        }
    }

}
