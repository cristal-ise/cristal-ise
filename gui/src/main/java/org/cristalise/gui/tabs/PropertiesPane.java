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
/*
 * StatusPane.java
 *
 * Created on March 20, 2001, 3:30 PM
 */

package org.cristalise.gui.tabs;

import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;

import java.awt.Color;
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

import org.cristalise.gui.ItemDetails;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.tree.NodeAgent;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.persistency.C2KLocalObjectMap;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

/**
 * Pane to display all work orders that this agent can execute, and activate them on 
 * request from the user. Subscribes to NodeItem for Property objects.
 */
@Slf4j
public class PropertiesPane extends ItemTabPane implements ActionListener {

    Box                     propertyBox;
    JButton                 eraseButton;
    boolean                 subbed      = false;
    HashMap<String, JLabel> loadedProps = new HashMap<String, JLabel>();
    DomainPathAdmin         domAdmin;
    JLabel                  roleTitle;
    RoleAdmin               roleAdmin   = null;

    public PropertiesPane() {
        super("Properties", "Properties");
        initPanel();

        // Create box container for properties
        propertyBox = Box.createVerticalBox();
        add(propertyBox);
        addGlue();

        if (MainFrame.isAdmin) {
            // role paths
            // Domain Paths
            roleTitle = getTitle("Roles");
            roleAdmin = new RoleAdmin();
            roleTitle.setVisible(false);
            roleAdmin.setVisible(false);
            addTitle(roleTitle);
            add(roleAdmin);

            // Domain Paths
            addTitle(getTitle("Domain Paths"));
            add(Box.createVerticalStrut(5));
            domAdmin = new DomainPathAdmin();
            add(domAdmin);

            if (Gateway.getProperties().getBoolean("EnableItemErase")) {
                addGlue();
                Box eraseBox = Box.createHorizontalBox();
                eraseBox.add(Box.createHorizontalGlue());
                eraseButton = new JButton("Erase!");
                eraseButton.addActionListener(this);
                eraseButton.setAlignmentX(RIGHT_ALIGNMENT);
                eraseButton.setBackground(Color.RED);
                eraseBox.add(eraseButton);
                add(eraseBox);
            }
            addGlue();
        }
    }
    
    @Override
    public void setParent(ItemDetails parent) {
        super.setParent(parent);

        Vertx vertx = Gateway.getVertx();
        vertx.eventBus().localConsumer(parent.getItemPath().getUUID() + "/" + PROPERTY, message -> {
            String[] tokens = ((String) message.body()).split(":");
            String propName = tokens[0];

            vertx.executeBlocking(promise -> {
                try {
                    if (tokens[1].equals("DELETE")) {
                        remove(propName);
                    }
                    else {
                        add((Property)sourceItem.getItem().getObject(PROPERTY+"/"+propName));
                    }
                }
                catch (ObjectNotFoundException e) {
                    log.error("", e);
                }
                promise.complete();
            }, res -> {
                //
            });
        });
    }

    @Override
    public void reload() {
        Gateway.getStorage().clearCache(sourceItem.getItemPath(), ClusterType.PROPERTY.getName());
        loadedProps = new HashMap<String, JLabel>();
        initForItem(sourceItem);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Property Pane Builder");
        if (sourceItem instanceof NodeAgent && roleAdmin != null) {
            roleAdmin.setEntity((AgentProxy) sourceItem.getItem());
            roleTitle.setVisible(true);
            roleAdmin.setVisible(true);
        }
        if (domAdmin != null) domAdmin.setEntity(sourceItem.getItem());
        propertyBox.removeAll();
        revalidate();
        
        try {
            @SuppressWarnings("unchecked")
            //Load all properties
            C2KLocalObjectMap<Property> propMap = (C2KLocalObjectMap<Property>)sourceItem.getItem().getObject(PROPERTY);
            for (String propName: propMap.keySet()) add(propMap.get(propName));
        }
        catch (ObjectNotFoundException e) {
            log.error("Could not load all the ItemProperties", e);
        }
    }

    /**
     *
     */
    public void add(Property newProp) {
        JLabel propLabel = loadedProps.get(newProp.getName());
        if (propLabel == null) { // new prop
            JPanel summaryPanel = new JPanel(new GridLayout(0, 2));
            summaryPanel.add(new JLabel(newProp.getName() + ":"));
            Box valueBox = Box.createHorizontalBox();
            propLabel = new JLabel(newProp.getValue());
            loadedProps.put(newProp.getName(), propLabel);
            valueBox.add(propLabel);
            if (MainFrame.isAdmin && newProp.isMutable()) {
                JButton editButton = new JButton("...");
                editButton.setMargin(new Insets(0, 0, 0, 0));
                editButton.setActionCommand(newProp.getName());
                editButton.addActionListener(this);
                valueBox.add(Box.createHorizontalStrut(7));
                valueBox.add(editButton);
                valueBox.add(Box.createHorizontalGlue());

            }
            summaryPanel.add(valueBox);
            propertyBox.add(Box.createVerticalStrut(7));
            propertyBox.add(summaryPanel);
        }
        propLabel.setText(newProp.getValue());
        revalidate();
    }

    public void remove(String id) {
        String propName = id.substring(id.lastIndexOf("/") + 1);
        JLabel propbox = loadedProps.get(propName);
        if (propbox != null) propbox.setText("[DELETED]");
        revalidate();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == eraseButton) {
            try {
                if (JOptionPane.showConfirmDialog(this,
                        "Are you sure?",
                        "Erase Item",
                        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return;
                String predefStep = sourceItem instanceof NodeAgent ? "RemoveAgent" : "Erase";
                MainFrame.userAgent.execute(sourceItem.getItem(), predefStep, new String[0]);
            }
            catch (Exception ex) {
                MainFrame.exceptionDialog(ex);
            }
        }
        else {
            String oldVal = loadedProps.get(e.getActionCommand()).getText();
            String newVal = (String) JOptionPane.showInputDialog(null, "Enter new value for " + e.getActionCommand(), "Edit Property",
                    JOptionPane.QUESTION_MESSAGE, null, null, oldVal);
            if (newVal != null && !(newVal.equals(oldVal))) {
                try {
                    (sourceItem.getItem()).setProperty(MainFrame.userAgent, e.getActionCommand(), newVal);
                }
                catch (Exception ex) {
                    MainFrame.exceptionDialog(ex);
                }
            }
        }
    }
}
