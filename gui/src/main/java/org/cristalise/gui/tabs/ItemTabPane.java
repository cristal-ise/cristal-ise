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
 * TabbedPane.java
 *
 * Created on March 22, 2001, 11:39 AM
 */
package org.cristalise.gui.tabs;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.ItemDetails;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.tabs.outcome.OutcomeHandler;
import org.cristalise.gui.tabs.outcome.form.OutcomePanel;
import org.cristalise.gui.tree.NodeItem;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

/**
 * Generic item details tabbed pane.
 */
@SuppressWarnings("serial")
@Slf4j
public class ItemTabPane extends JPanel implements Runnable {
    protected NodeItem         sourceItem;
    protected String           titleText    = null;
    protected ImageIcon        titleIcon    = null;
    private final String       tabName;
    public static Font         titleFont    = null;
    public static Color        headingColor = new Color(0, 0, 185);
    protected ItemDetails      parent;
    protected static ImageIcon mReloadIcon  = null;
    protected Box              titleBox;
    static {
        try {
            mReloadIcon = ImageLoader.findImage("refresh.png");
        }
        catch (Exception e) {
            log.warn("Couldn't load images: " + e);
        }
    }

    public void focusLost(FocusEvent e) {
    }

    public ItemTabPane(String tabName, String titleText) {
        this.tabName = tabName;
        this.titleText = titleText == null ? null : titleText;
        if (titleFont == null)
            titleFont = new Font("SansSerif", Font.BOLD, this.getFont().getSize() + 5);
        log.debug("ItemTabPane.<init> - viewing " + tabName);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void setParent(ItemDetails parent) {
        this.parent = parent;
    }

    public String getTabName() {
        return tabName;
    }

    protected JLabel getTitle(String titleText) {
        if (titleIcon == null)
            titleIcon = ImageLoader.findImage("info_16.png");
        JLabel title = new JLabel(titleText, titleIcon, SwingConstants.LEFT);
        title.setFont(titleFont);
        title.setForeground(headingColor);
        return title;
    }

    protected void addTitle(JLabel title) {
        Box labelBox = Box.createHorizontalBox();
        labelBox.add(title);
        labelBox.add(Box.createHorizontalGlue());
        add(labelBox);
    }

    protected void addGlue() {
        add(Box.createVerticalGlue());
    }

    protected void initPanel() {
        // Help panel
        if (titleText == null)
            titleText = tabName;

        JLabel title = getTitle(titleText);

        JButton refreshButton = new JButton(mReloadIcon);
        refreshButton.setToolTipText("Refresh");
        refreshButton.setMargin(new Insets(0, 0, 0, 0));
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCursor(new Cursor(Cursor.WAIT_CURSOR));
                reload();
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        String defaultStartTab = MainFrame.getPref("DefaultStartTab", "Properties");
        JToggleButton defaultStart = new JToggleButton(ImageLoader.findImage("graph/start.png"));
        defaultStart.setMargin(new Insets(0, 0, 0, 0));
        defaultStart.setToolTipText(
                "Select this tab to be the default one opened when you double click an item");
        defaultStart.setSelected(tabName.equals(defaultStartTab));
        defaultStart.setActionCommand(tabName);
        defaultStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((JToggleButton) e.getSource()).isSelected())
                    MainFrame.setPref("DefaultStartTab", e.getActionCommand());
            }
        });
        titleBox = Box.createHorizontalBox();
        titleBox.add(title);
        titleBox.add(Box.createHorizontalGlue());
        titleBox.add(defaultStart);
        titleBox.add(refreshButton);
        this.add(titleBox);
        this.add(Box.createVerticalStrut(5));
    }

    public void initForItem(NodeItem sourceItem) {
        this.sourceItem = sourceItem;
        new Thread(this).start();
    }

    /**
     * Empty implementation. Subclases shall provide the cluster specific version
     */
    @Override
    public void run() {
        Thread.currentThread().setName("Default Entity Pane Builder");
        JLabel error = new JLabel("In Development");
        this.add(error);
    }

    public void reload() {
    }

    public void runCommand(String command) {
    }

    public void destroy() {
        parent = null;
    }

    static public OutcomeHandler getOutcomeHandler(String schema, int version) {
        String propName = "OutcomeHandler." + schema + "." + version;
        if (Gateway.getProperties().containsKey(propName))
            try {
                return (OutcomeHandler) Gateway.getProperties().getInstance(propName);
            }
            catch (Exception ex) {
                log.error("Error creating handler " + Gateway.getProperties().getString(propName) + ". using default outcome editor");
            }

        propName = "OutcomeHandler.*";
        if (Gateway.getProperties().containsKey(propName))
            try {
                return (OutcomeHandler) Gateway.getProperties().getInstance(propName);
            }
            catch (Exception ex) {
                log.error("Error creating handler " + Gateway.getProperties().getString(propName) + ". using default outcome editor");
            }
        return new OutcomePanel();
    }
}
