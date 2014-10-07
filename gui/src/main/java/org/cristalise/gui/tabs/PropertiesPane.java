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
/*
 * StatusPane.java
 *
 * Created on March 20, 2001, 3:30 PM
 */

package org.cristalise.gui.tabs;

/**
 * @author  abranson
 * @version
 */
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.tree.NodeAgent;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Language;


/**
 * Pane to display all work orders that this agent can execute, and activate
 * them on request from the user. Subscribes to NodeItem for Property objects.
 * @version $Revision: 1.44 $ $Date: 2005/08/31 07:21:20 $
 * @author  $Author: abranson $
 */
public class PropertiesPane extends ItemTabPane implements ProxyObserver<Property>, ActionListener {

    Box propertyBox;
    JButton eraseButton;
    boolean subbed = false;
    HashMap<String, JLabel> loadedProps = new HashMap<String, JLabel>();
    JLabel domTitle;
    DomainPathAdmin domAdmin;

    public PropertiesPane() {
        super("Properties", "Properties");
        initPanel();

        // Create box container for properties
        getGridBagConstraints();
        c.gridx = 0; c.gridy = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 1.0; c.weighty = 2.0;
        propertyBox = Box.createVerticalBox();
        gridbag.setConstraints(propertyBox, c);
        add(propertyBox);
        if (MainFrame.isAdmin) { // edit dompath
            c.gridy++;
            c.fill = GridBagConstraints.NONE;
            c.weighty=0.0;
            domTitle = new JLabel("Domain Paths", titleIcon, SwingConstants.LEFT);
            domTitle.setFont(titleFont);
            domTitle.setForeground(headingColor);
            gridbag.setConstraints(domTitle, c);
            add(domTitle);

            c.gridy++;
            c.fill = GridBagConstraints.BOTH;
            c.weighty=1.0;
            domAdmin = new DomainPathAdmin();
            gridbag.setConstraints(domAdmin, c);
            add(domAdmin);


            if (Gateway.getProperties().getBoolean("EnableItemErase")) {
                c.gridy++;
                c.fill = GridBagConstraints.NONE;
                eraseButton = new JButton(Language.translate("Erase!"));
                eraseButton.addActionListener(this);
                eraseButton.setActionCommand("Erase Item");
                gridbag.setConstraints(eraseButton, c);
                add(eraseButton);
            }
        }
    }

    @Override
	public void reload() {
    	Gateway.getStorage().clearCache(sourceItem.getItemPath(), ClusterStorage.PROPERTY);
        loadedProps = new HashMap<String, JLabel>();
        initForItem(sourceItem);
    }

    @Override
	public void run() {
        Thread.currentThread().setName("Property Pane Builder");
        if (sourceItem instanceof NodeAgent) {
            remove(domAdmin);
            remove(domTitle);
            eraseButton.setEnabled(false);
        }
        else if (domAdmin != null)
            domAdmin.setEntity(sourceItem.getItem());
        propertyBox.removeAll();
        propertyBox.add(Box.createGlue());
		revalidate();
        sourceItem.getItem().subscribe(new MemberSubscription<Property>(this, ClusterStorage.PROPERTY, true));

    }
    /**
     *
     */
    @Override
	public void add(Property newProp) {
        JLabel propLabel = loadedProps.get(newProp.getName());
        if (propLabel == null) { // new prop
            JPanel summaryPanel = new JPanel(new GridLayout(0,2));
            summaryPanel.add(new JLabel(Language.translate(newProp.getName()) + ":"));
            Box valueBox = Box.createHorizontalBox();
            propLabel = new JLabel(newProp.getValue());
            loadedProps.put(newProp.getName(), propLabel);
            valueBox.add(propLabel);
            if (MainFrame.isAdmin && newProp.isMutable()) {
                JButton editButton = new JButton("...");
                editButton.setMargin(new Insets(0,0,0,0));
                editButton.setActionCommand(newProp.getName());
                editButton.addActionListener(new ActionListener() {
                    @Override
					public void actionPerformed(ActionEvent e){
                        String oldVal = loadedProps.get(e.getActionCommand()).getText();
                        String newVal = (String)JOptionPane.showInputDialog(null, "Enter new value for "+e.getActionCommand(), "Edit Property",
                            JOptionPane.QUESTION_MESSAGE, null, null, oldVal);
                        if (newVal!=null && !(newVal.equals(oldVal))) {
                            try {
                                (sourceItem.getItem()).setProperty(MainFrame.userAgent, e.getActionCommand(), newVal);
                            } catch (Exception ex) {
                            	MainFrame.exceptionDialog(ex);
                            }
                        }
                    }
                });
                valueBox.add(Box.createVerticalStrut(7));
                valueBox.add(editButton);

            }
            summaryPanel.add(valueBox);
            propertyBox.add(Box.createVerticalStrut(7));
            propertyBox.add(summaryPanel);
        }
        propLabel.setText(newProp.getValue());
        revalidate();
    }

    @Override
	public void remove(String id) {
        String propName = id.substring(id.lastIndexOf("/")+1);
        JLabel propbox = loadedProps.get(propName);
        if (propbox!= null) propbox.setText("DELETED");
        revalidate();
    }

    @Override
	public void actionPerformed(ActionEvent e) {
        String[] params;
        String predefStep;

        if (JOptionPane.showConfirmDialog(this,
                "Are you sure?",
                e.getActionCommand(),
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;

        if (e.getActionCommand().equals("Erase Item")) {
            params = new String[0];
            predefStep = "Erase";
        }
        else
            return;

        try {
            MainFrame.userAgent.execute(sourceItem.getItem(), predefStep, params);
        } catch (Exception ex) {
            MainFrame.exceptionDialog(ex);
        }
    }

	@Override
	public void control(String control, String msg) {
		// TODO Auto-generated method stub
		
	}

}
