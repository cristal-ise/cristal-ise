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
package org.cristalise.gui.tree;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.cristalise.gui.ItemDetails;
import org.cristalise.gui.ItemTabManager;
import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


/**
 * Structure for Item presence on the tree and ItemDetails boxes. Created by NodeFactory.
 * @author $Author: abranson $
 * @version $Version$
 */
public class NodeItem extends Node implements Transferable {

	protected ItemProxy myItem = null;

    public NodeItem(Path path, ItemTabManager desktop) {
        
    	super(path, desktop);
        Logger.msg(2,"NodeEntity.<init> - Creating item for '"+path.toString()+"'.");

        // if an item - resolve the item and get its properties
		try {
			myItem = Gateway.getProxyManager().getProxy(path);
	        this.itemPath = path.getItemPath();
	        Logger.msg(2,"NodeEntity.<init> - System key is "+this.itemPath);
	
	        // Name should be the alias if present
	        String alias = myItem.getName();
	        if (alias != null) this.name = alias;
	
	        try {
				this.type = myItem.getProperty("Type");
			} catch (ObjectNotFoundException e) {
				this.type = "";
			}
	        String iconString = this.type;
	        if (type.equals("ActivityDesc"))
				try {
					iconString = myItem.getProperty("Complexity")+iconString;
				} catch (ObjectNotFoundException e) {
					iconString = "error";
				}
	        iconString = iconString.toLowerCase();
	        this.setIcon(iconString);
		} catch (ObjectNotFoundException e1) {
			this.itemPath = null;
			this.type="Error";
			this.name="Entity not found";
			this.setIcon("error");
		}
        createTreeNode();
        makeExpandable();
    }

    public ItemProxy getItem() {
        return myItem;
    }
    
    public void openItem() {
        desktop.add(this);
    }
    
	public Collection<? extends CollectionMember> getParentCollection() {
		return parentCollection;
	}

	public Integer getSlotNo() {
		return slotNo;
	}

	Collection<? extends CollectionMember> parentCollection;
	Integer slotNo = null;
	static DataFlavor dataFlavor = new DataFlavor(NodeItem.class, "NodeItem");
	ItemProxy parentItem;
	static DataFlavor[] supportedFlavours = new DataFlavor[] { 
		dataFlavor, 
		new DataFlavor(Path.class, "Path"),
		DataFlavor.getTextPlainUnicodeFlavor() };

    
    public void setCollection(Collection<? extends CollectionMember> parentCollection, Integer slotNo, ItemProxy parentItem) {
    	this.parentCollection = parentCollection;
    	this.slotNo = slotNo;
    	this.parentItem = parentItem;
	}

    @Override
	public void loadChildren() {
        try {
            String collections = myItem.queryData("Collection/all");
            StringTokenizer tok = new StringTokenizer(collections, ",");
            while (tok.hasMoreTokens()) {
                NodeCollection newCollection = new NodeCollection(myItem, tok.nextToken(), desktop);
                add(newCollection);
            }
            end(false);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

	@Override
	public JPopupMenu getPopupMenu() {
		
        JPopupMenu popup = super.getPopupMenu();
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
                openItem();
            }
        });
        popup.addSeparator();
        popup.add(openItem);
		popup.addSeparator();
		if (parentCollection != null && MainFrame.isAdmin) {
			JMenuItem collMenuItem = new JMenuItem("Remove from collection");
			//collMenuItem.setActionCommand("removeColl");
			collMenuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String[] params = { parentCollection.getName(), String.valueOf(slotNo) };
					String predefStepName = parentCollection instanceof Aggregation?"ClearSlot":"RemoveSlotFromCollection";
					try {
						MainFrame.userAgent.execute(parentItem, predefStepName, params);
					} catch (Exception e1) {
						MainFrame.exceptionDialog(e1);
					}
					
				}
			});
			popup.add(collMenuItem);
			popup.addSeparator();
		}
		try {
            ArrayList<Job> jobList = myItem.getJobList(MainFrame.userAgent);
            ArrayList<String> already = new ArrayList<String>();
			if (jobList.size() > 0) {
	            for (Job thisJob : jobList) {
	                String stepName = thisJob.getStepName();
					if (already.contains(stepName))
						continue;
					already.add(stepName);
					JMenuItem menuItem = new JMenuItem(stepName);
					menuItem.setActionCommand(stepName);
					menuItem.addActionListener(new ActionListener() {
                        @Override
						public void actionPerformed(ActionEvent e) {
                            execute(e.getActionCommand());
                        }
                    });
					popup.add(menuItem);

				}
			}
            else {
    			JMenuItem noAct = new JMenuItem("No activities");
	   		    noAct.setEnabled(false);
		      	popup.add(noAct);
            }
		} catch (Exception ex) {
			JMenuItem error = new JMenuItem("Error querying jobs");
			error.setEnabled(false);
			popup.add(error);
		}

		return popup;
	}

	public void execute(String stepName) {
		ItemDetails thisDetail = desktop.add(this);
		thisDetail.runCommand("Execution", stepName);
	}

	public ArrayList<String> getTabs() {

        ArrayList<String> requiredTabs = new ArrayList<String>();
        requiredTabs.add("Properties");
        try {
            String collNames = myItem.queryData(ClusterStorage.COLLECTION+"/all");
            if (collNames.length() > 0)
                requiredTabs.add("Collection");
        } catch (Exception e) { }
        requiredTabs.add("Execution");
        requiredTabs.add("History");
        requiredTabs.add("Viewpoint");
        requiredTabs.add("Workflow");
        return requiredTabs;

    }

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return supportedFlavours;
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (DataFlavor flavour : supportedFlavours) {
			if (flavour.equals(flavor))
				return true;
		}
		return false;
	}

	@Override
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (flavor.equals(supportedFlavours[0]))
				return this;
		if (flavor.equals(supportedFlavours[1]))
				return binding;
		if (flavor.equals(supportedFlavours[2]))
				return name; 
		throw new UnsupportedFlavorException(flavor);
	}
}
