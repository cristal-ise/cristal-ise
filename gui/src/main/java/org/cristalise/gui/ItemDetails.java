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
package org.cristalise.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import org.cristalise.gui.tabs.ItemTabPane;
import org.cristalise.gui.tree.NodeItem;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.Logger;


/**
 * The tab pane for each viewed item
 * @version $Revision: 1.38 $ $Date: 2005/06/27 15:16:14 $
 * @author  $Author: abranson $
 */
public class ItemDetails extends JPanel implements ChangeListener, Runnable {
    protected JTabbedPane myTabbedPane = new JTabbedPane(SwingConstants.TOP);
    protected JPanel itemTitlePanel;
    private ItemTabManager desktopManager;
    protected NodeItem myItem;
    protected HashMap<ItemTabPane, Boolean> childPanes = new HashMap<ItemTabPane, Boolean>();
    protected String startTab;
    protected String startCommand = null;
    protected boolean initialized = false;

    public ItemDetails(NodeItem thisItem) {
        super();
        startTab = MainFrame.getPref("DefaultStartTab", "Properties");
        myItem = thisItem;
    }

    @Override
	public void run() {
        Thread.currentThread().setName("Entity Pane Builder");
        ItemTabPane componentToAdd = null;
        setLayout(new BorderLayout());
		itemTitlePanel = getItemTitlePanel();
       	add(itemTitlePanel, BorderLayout.NORTH);
        add(myTabbedPane);

        // decide which tabs to create
        ArrayList<?> requiredTabs = myItem.getTabs();

        for (Object name2 : requiredTabs) {
            String tabName = (String)name2;
            if (tabName != null) {
                //create class instances and initialise
                Class<?> myClass = null;
                //look up the required TabbedPane
                try {
                    myClass = Class.forName(this.getClass().getPackage().getName() + ".tabs." + tabName + "Pane");
                    Logger.msg(2, "ItemDetails.<init> - Creating ItemTabPane instance: " +
                        this.getClass().getPackage().getName() + ".tabs." + tabName + "Pane");
                    componentToAdd = (ItemTabPane)myClass.newInstance();
                } catch (ClassNotFoundException e) {
                    Logger.msg(2, "ItemDetails.<init> - No specialist tab found for " + tabName + ". Using default.");
                } catch (InstantiationException e) {
                    Logger.msg(0, "ItemDetails.<init> - Instantiation Error! " + e);
                } catch (IllegalAccessException e) {
                    Logger.msg(0, "ItemDetails.<init> - Illegal Method Access Error! Class was probably not a ItemTabPane: " + e);
                }
                if (componentToAdd == null) componentToAdd = new ItemTabPane(tabName, null);
                componentToAdd.setParent(this);

                //adds the component to the panel
                childPanes.put(componentToAdd, new Boolean(false));

                int placement = myTabbedPane.getTabCount();
                if (tabName.equals("Properties")) // must be first
                    placement = 0;
                myTabbedPane.insertTab(componentToAdd.getTabName(), null, componentToAdd, null, placement);
            }
        }
        initialized = true;
        if (!(requiredTabs.contains(startTab))) {
            startTab = "Properties";
            startCommand = null;
        }
        runCommand(startTab, startCommand);
        myTabbedPane.setVisible(true);
        myTabbedPane.addChangeListener(this);
        validate();
        MainFrame.progress.stopBouncing("Done");

    }

    @Override
	public void stateChanged(javax.swing.event.ChangeEvent p1) {
        initialisePane((ItemTabPane)myTabbedPane.getSelectedComponent());
    }

    public void initialisePane(ItemTabPane pane) {
        Boolean isInit = childPanes.get(pane);
        if (isInit.booleanValue() == false) {
            Logger.msg(4,"Initialising "+pane.getTabName());
            pane.initForItem(myItem);
            childPanes.put(pane, new Boolean(true));
            validate();
        }
    }

    public ItemTabManager getDesktopManager() {
        return desktopManager;
    }

    public void setDesktopManager(ItemTabManager newDesktopManager) {
        desktopManager = newDesktopManager;
    }

    public JPanel getItemTitlePanel() {
        JPanel titlePanel = new JPanel();
        JComponent current;
        // Use gridbag layout for title
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        titlePanel.setLayout(gridbag);
        // Place Item Icon
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTH;
        c.ipadx = 5;
        c.ipady = 5;
        ImageIcon icon = ImageLoader.findImage("typeicons/"+myItem.getIconName()+"_32.png");
        if (icon==ImageLoader.nullImg) icon = ImageLoader.findImage("typeicons/item_32.png");
        current = new JLabel(icon);
        gridbag.setConstraints(current, c);
        titlePanel.add(current);
        // Place Name/ID Label
        current = new JLabel(myItem.getName() + " (" + myItem.getItemPath().getUUID().toString() + ")");
        c.gridx = 1; c.gridy = 0; c.gridheight = 1;
        c.anchor = GridBagConstraints.NORTH; c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0; c.ipadx = 2; c.ipady = 2;
        current.setFont(new Font("Helvetica", Font.PLAIN, 18));
        gridbag.setConstraints(current, c);
        titlePanel.add(current);
        // Place Type Label
        current = new JLabel(myItem.getType());
        c.gridx = 1; c.gridy = 2; c.gridheight = 1;
        c.anchor = GridBagConstraints.CENTER; c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        current.setFont(new Font("Helvetica", Font.PLAIN, 12));
        gridbag.setConstraints(current, c);
        titlePanel.add(current);
        return titlePanel;
    }

    public void discardTabs() {
        myTabbedPane.removeChangeListener(this);
        myTabbedPane.removeAll();
        for (Iterator<ItemTabPane> iter = childPanes.keySet().iterator(); iter.hasNext();) {
            ItemTabPane element = iter.next();
            element.destroy();
            iter.remove();
        }
    }

    public ItemPath getItemPath()
    {
    	return myItem.getItemPath();
    }

    public void closeTab() {
    	desktopManager.remove(myItem.getItemPath());
    	Logger.msg(5,"Remove master Tab :"+myItem.getType()+ " SysKey "+myItem.getItemPath());
	}

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("close"))
			closeTab();
    }

    public void runCommand(String tab, String command) {
    	if (initialized) {
    		int tabIndex = findTab(tab);
            Logger.msg(3, "Running command "+tab+" "+command+" ("+tabIndex+")");
    		if (tabIndex == -1) {
                Logger.error("Tab "+tab+" not found for command "+command);
                return;
            }
			ItemTabPane startPane = (ItemTabPane)myTabbedPane.getComponentAt(tabIndex);
			myTabbedPane.setSelectedIndex(tabIndex);
			initialisePane(startPane);
			if (command!= null) startPane.runCommand(command);
		}
		else
		{
            Logger.msg(3, "Storing command "+tab+" "+command+" until initialised.");
    		startTab = tab;
			startCommand = command;
		}
    }

    protected int findTab(String tabName) {
    	for (int i=0; i< myTabbedPane.getTabCount(); i++) {
            ItemTabPane thisPane = (ItemTabPane)myTabbedPane.getComponentAt(i);
    		if (thisPane.getTabName().equals(tabName))
				return i;
        }
		return -1;
    }


    public void refresh()
    {
    }
    /**
     *
     */
    @Override
	protected void finalize() throws Throwable {
        Logger.msg(7, "EntityDetails "+myItem.getItemPath()+" reaped");
        super.finalize();
    }

}