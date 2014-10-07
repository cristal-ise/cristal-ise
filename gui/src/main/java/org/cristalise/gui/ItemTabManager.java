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
package org.cristalise.gui;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.cristalise.gui.tabs.JTabbedPaneWithCloseIcons;
import org.cristalise.gui.tree.NodeItem;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.Logger;


/**
 * Keeps
 *
 * @version $Revision: 1.12 $ $Date: 2005/09/12 14:56:19 $
 * @author  $Author: abranson $
 */

public class ItemTabManager extends JPanel
{
    
    protected HashMap<ItemPath, ItemDetails> openItems = new HashMap<ItemPath, ItemDetails>();
    protected JTabbedPaneWithCloseIcons tabbedPane = new JTabbedPaneWithCloseIcons();
	//JTabbedPane tabbedPane = new JTabbedPane();
    MenuBuilder myMenuBuilder;


    public ItemTabManager() {
        super();
        setLayout(new GridLayout(1,1));
        setBorder(BorderFactory.createLoweredBevelBorder());

        add(tabbedPane);
    }

    public ItemDetails add(NodeItem thisItem) {

        ItemDetails requestedDetails;
        if (!openItems.containsKey(thisItem.getItemPath())) {
            MainFrame.progress.startBouncing("Opening "+thisItem.getName()+". Please wait.");
        	Logger.msg(1, "ItemWindowManager.add() - Window for syskey "+thisItem.getItemPath()+" not found. Opening new one.");
            requestedDetails = new ItemDetails(thisItem);
            Thread itemLoader = new Thread(requestedDetails);
            itemLoader.start();
            openItems.put(thisItem.getItemPath(), requestedDetails);
            requestedDetails.setDesktopManager(this);

            // get currently selected item to set location
            tabbedPane.addTab(thisItem.getName(), thisItem.getIcon(), requestedDetails, thisItem.getType());

        }
        else { //opened window but different nodeitem
            requestedDetails = openItems.get(thisItem.getItemPath());
        }
        tabbedPane.setSelectedComponent(requestedDetails);
        return requestedDetails;
    }

    public void setMenuBuilder(MenuBuilder myMenuBuilder) {
        this.myMenuBuilder = myMenuBuilder;
    }

	public void remove(ItemPath itemPath) {
        if (!openItems.containsKey(itemPath)) return;
        ItemDetails tabToClose = openItems.get(itemPath);
        tabbedPane.remove(tabToClose);
        tabToClose.discardTabs();
        openItems.remove(itemPath);
    }

    public void closeAll(boolean keepOpen) {
    	ArrayList<ItemPath> toRemove = new ArrayList<ItemPath>();
    	for (ItemPath element : openItems.keySet()) {
			if (keepOpen && openItems.get(element).equals(tabbedPane.getSelectedComponent())) continue;
			toRemove.add(element);
		}
    	for (ItemPath element : toRemove) {
			remove(element);
		}
    }
}
