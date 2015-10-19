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
package org.cristalise.gui.tabs.outcome.form.field;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;

import org.exolab.castor.xml.schema.SimpleType;


/**************************************************************************
 *
 * $Revision: 1.7 $
 * $Date: 2006/05/24 07:51:51 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class ArrayEditField extends EditField implements ActionListener {

    Box arrayBox;
    Box expandBox;
    Box editBox;
    JScrollPane arrayView;
    JButton arrayButton;
    JButton expandButton;
    JButton contractButton;
    JButton addButton;
    JButton removeButton;
    ArrayTableModel arrayModel;
    JLabel arrayLabel = new JLabel("Array");
    boolean panelShown = false;
    boolean readOnly = false;

    public ArrayEditField(SimpleType type) {
        arrayBox = Box.createVerticalBox();
        arrayBox.add(arrayLabel);
        arrayButton = new JButton("Show");
        arrayButton.addActionListener(this);
        arrayButton.setActionCommand("toggle");
        arrayBox.add(arrayButton);

        expandBox = Box.createHorizontalBox();
        expandButton = new JButton(">>");
        expandButton.setToolTipText("Increase the number of columns displaying this array");
        expandButton.addActionListener(this);
        expandButton.setActionCommand("extend");

        contractButton = new JButton("<<");
        contractButton.setToolTipText("Decrease the number of columns displaying this array");
        contractButton.addActionListener(this);
        contractButton.setActionCommand("contract");

        expandBox.add(contractButton);
        expandBox.add(Box.createHorizontalGlue());
        expandBox.add(expandButton);

        arrayModel = new ArrayTableModel(type);
        if (arrayModel.getColumnCount() < 2) contractButton.setEnabled(false);
        arrayView = new JScrollPane(new JTable(arrayModel));

        editBox = Box.createHorizontalBox();
        addButton = new JButton("+");
        addButton.setToolTipText("Add a field to the end of this array");
        addButton.addActionListener(this);
        addButton.setActionCommand("add");
        removeButton = new JButton("-");
        removeButton.setToolTipText("Remove the last field from this array");
        removeButton.addActionListener(this);
        removeButton.setActionCommand("remove");
        editBox.add(addButton);
        editBox.add(Box.createHorizontalGlue());
        editBox.add(removeButton);
    }
    /**
     *
     */
    @Override
	public String getDefaultValue() {
        return "";
    }
    /**
     *
     */
    @Override
	public String getText() {
        return arrayModel.getData();
    }
    /**
     *
     */
    @Override
	public void setText(String text) {
        arrayModel.setData(text);
        arrayLabel.setText("Array ("+arrayModel.getArrayLength()+" values)");
    }
    /**
     *
     */
    @Override
	public Component getControl() {
        return arrayBox;
    }
    /**
     *
     */
    @Override
	public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("toggle")) {
            arrayBox.removeAll();
            if (panelShown) {
                arrayBox.add(arrayLabel);
                arrayBox.add(Box.createVerticalStrut(7));
                arrayBox.add(arrayButton);
                arrayButton.setText("Show");
            }
            else {
                arrayBox.add(arrayLabel);
                arrayBox.add(Box.createVerticalStrut(7));
                arrayBox.add(arrayButton);
                arrayBox.add(Box.createVerticalStrut(7));
                arrayBox.add(expandBox);
                arrayBox.add(Box.createVerticalStrut(7));
                arrayBox.add(arrayView);
                if (!readOnly) arrayBox.add(editBox);
                arrayButton.setText("Hide");
            }
            panelShown = !panelShown;
            arrayBox.validate();
        }
        else if (e.getActionCommand().equals("add")) {
        	arrayModel.addField();
        	arrayLabel.setText("Array ("+arrayModel.getArrayLength()+" values)");
        }
        else if (e.getActionCommand().equals("remove")) {
        	arrayModel.removeField();
        	arrayLabel.setText("Array ("+arrayModel.getArrayLength()+" values)");
        }
        else {
            int currentCols = arrayModel.getColumnCount();
            if (e.getActionCommand().equals("extend"))
                currentCols++;
            else if (e.getActionCommand().equals("contract"))
                currentCols--;
            arrayModel.setColumnCount(currentCols);
            contractButton.setEnabled(currentCols > 1);
        }

    }

    /**
     *
     */
    @Override
	public JTextComponent makeTextField() {
        // not used by array
        return null;
    }
	@Override
	public void setEditable(boolean editable) {
		readOnly = !editable;
		arrayModel.setReadOnly(!readOnly);
	}

}
