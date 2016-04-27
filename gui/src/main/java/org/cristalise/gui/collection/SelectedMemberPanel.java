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
package org.cristalise.gui.collection;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.cristalise.gui.DomainKeyConsumer;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.graph.view.SelectedVertexPanel;
import org.cristalise.kernel.collection.AggregationMember;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;


/**************************************************************************
 *
 * $Revision: 1.10 $
 * $Date: 2005/05/12 10:12:52 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/


public class SelectedMemberPanel extends SelectedVertexPanel implements DomainKeyConsumer {

	JLabel slotNumber = new JLabel();
    JTextField memberKey = new JTextField(14);

	JButton findButton = new JButton("Find");
    JToggleButton changeButton = new JToggleButton("Change");
    JButton removeButton = new JButton("Remove");

    SelectedMemberPanel me;
    AggregationMember selectedMember = null;

	public SelectedMemberPanel() {
        me=this;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel attrs = new JPanel(new GridLayout(3,2));
        attrs.add(new JLabel("Slot Number:"));
        attrs.add(slotNumber);
        attrs.add(new JLabel("Assigned Member:"));
        attrs.add(memberKey);
        memberKey.setEditable(false);

        add(attrs);
        add(Box.createVerticalStrut(10));

		findButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				String code = memberKey.getText();
				if (code == null || code.length() == 0)
					code = memberKey.getText().replace('/',' ');
				MainFrame.itemFinder.pushNewKey(code);
			}
		});

        changeButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent ae) {
                if (changeButton.getModel().isSelected()) {
                    MainFrame.itemFinder.setConsumer(me, "Assign");
                    findButton.setEnabled(false);
                }
                else {
                    MainFrame.itemFinder.clearConsumer(me);
                    if (selectedMember.getItemPath() != null) findButton.setEnabled(true);
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent ae) {
                selectedMember.clearItem();
                selectedMember.getProperties().remove("Name");
                select(selectedMember);
            }
        });

		Box buttonBox = Box.createHorizontalBox();
		buttonBox.add(findButton);
        if (MainFrame.isAdmin) {
    		buttonBox.add(changeButton);
	   	    buttonBox.add(removeButton);
        }

        setButtons(false);
		add(buttonBox);
	}

	@Override
	public void select(Vertex vert) {
        selectedMember = (AggregationMember)vert;
		slotNumber.setText(String.valueOf(vert.getID()));
        ItemPath memberPath = selectedMember.getItemPath();
        String name = "Empty";
        if (memberPath != null)
        	try {
        		ItemProxy member = Gateway.getProxyManager().getProxy(memberPath);
        		name = member.getName();
        	} catch (Exception e) { }
        memberKey.setText(name);
        setButtons(true);

		revalidate();
	}

	@Override
	public void clear() {
		slotNumber.setText("");
        memberKey.setText("");
        setButtons(false);
		revalidate();
	}

    public void setButtons(boolean state) {
        findButton.setEnabled(state);
        changeButton.getModel().setSelected(false);
        changeButton.setEnabled(state);
        removeButton.setEnabled(state);
        MainFrame.itemFinder.clearConsumer(me);
    }
    /**
     *
     */
    @Override
	public void push(DomainPath key) {
        try {
            selectedMember.assignItem(key.getItemPath());
            select(selectedMember);
        } catch (InvalidCollectionModification ex) {
            JOptionPane.showMessageDialog(null, "Item does not fit in this slot", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ObjectNotFoundException e) {
        	JOptionPane.showMessageDialog(null, "Not an item - cannot assign", "Error", JOptionPane.ERROR_MESSAGE);
		}
    }

    /**
     *
     */
    @Override
	public void push(String name) {
        JOptionPane.showMessageDialog(null, "Item is not known in this centre", "Error", JOptionPane.ERROR_MESSAGE);
    }

}

