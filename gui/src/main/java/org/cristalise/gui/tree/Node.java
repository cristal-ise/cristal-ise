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
package org.cristalise.gui.tree;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;

import org.cristalise.gui.DynamicTreeBuilder;
import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.ItemTabManager;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;


public abstract class Node implements Runnable {

    protected Path binding;
    protected DefaultMutableTreeNode treeNode;
    protected String name; // domain key
    protected ItemPath itemPath; // target item
    // attributes
    protected String type = "";
    protected String toolTip = null;
    protected Icon icon;
    protected boolean isExpandable = false;
    protected List<Node> childNodes = new ArrayList<>();
    protected ArrayList<NodeSubscriber> subscribers = new ArrayList<NodeSubscriber>();
    protected DynamicTreeBuilder loader = null;
    private boolean loaded = false;
    private String iconName;
    protected ItemTabManager desktop;
    static ImageIcon folder = ImageLoader.findImage("folder.png");
    static ImageIcon emptyLeaf = ImageLoader.findImage("leaf.png");

    public Node(ItemTabManager desktop) {
    	this.desktop = desktop;
    }

    protected void createTreeNode() {
        this.treeNode = new DefaultMutableTreeNode(this);
    }

    public Node(Path path, ItemTabManager desktop) {
        this.binding = path;
        this.desktop = desktop;
        try {
			this.itemPath = path.getItemPath();
		} catch (ObjectNotFoundException e) { }
        // get the name of this node (last path element)
        String[] pathComponents = path.getPath();
        if (pathComponents.length > 0)
            this.name = pathComponents[pathComponents.length-1];
        else
            this.name = Gateway.getProperties().getProperty("Name");
    }

    public ItemTabManager getDesktop() {
        return desktop;
    }

    public Node newNode(Path path)
    {
        try {
            if (path.getItemPath() instanceof AgentPath)
                return new NodeAgent(path, desktop);
            else if (path instanceof RolePath)
                return new NodeRole(path, desktop);
            return new NodeItem(path, desktop);
        } catch (ObjectNotFoundException ex) {
            return new NodeContext(path, desktop);
        }

    }

    /** Inserts a tree builder as the first child of the node, so it can be opened in the tree
    */
    public void makeExpandable() {
        if (isExpandable) return;
        loader = new DynamicTreeBuilder(this.treeNode, desktop);
        this.treeNode.insert(loader.getTreeNode(),0);
        isExpandable = true;
    }


    public DefaultMutableTreeNode getTreeNode() {
        return treeNode;
    }

    public void setTreeNode(DefaultMutableTreeNode treeNode) {
        this.treeNode = treeNode;
        treeNode.setUserObject(this);
    }

    /** Subscription for loading node children.
      * Note this is separate from the itemproxy subscription as it included query of the naming service
      * and eventually should not require access to the item at all for higher performance */
    public void subscribeNode(NodeSubscriber target) {
        subscribers.add(target);
        if (loaded == false) {
            loaded = true;
            loadMore();
        }
        else {
            synchronized (childNodes) {
                for (Node node : childNodes) target.add(node);
            }
        }
    }

    public void loadMore() {
        Thread loading = new Thread(this);
        loading.start();
    }

    public void unsubscribeNode(NodeSubscriber target) {
        subscribers.remove(target);
    }

    public void add(Node newNode) {
        synchronized(childNodes) {
            childNodes.add(newNode);
            for (NodeSubscriber thisSub : subscribers) {
                thisSub.add(newNode);
            }
        }
    }

    public void remove(Path oldPath) {
        synchronized(childNodes) {
            for (NodeSubscriber thisSub : subscribers) {
                thisSub.remove(oldPath);
            }
        }
    }

    public void removeAllChildren() {
        synchronized(childNodes) {
            for (Node node : childNodes) {
                remove(node.getPath());
            }
        }
    }

    public Node getChildNode(Path itsPath) {
        for (Node node : childNodes) {
            if ( node.getPath().equals(itsPath) ) return node;
        }
        return null;
    }

    // end of current batch
    public void end(boolean more) {
        for (NodeSubscriber thisSub : subscribers) {
            thisSub.end(more);
        }
    }


    @Override
	public void run() {
        Thread.currentThread().setName("Node Loader: "+name);
        loadChildren();
    }

    public abstract void loadChildren();

    public void refresh() {
        removeAllChildren();
        loadChildren();
    }

    // Getters and Setters

    public ItemPath getItemPath() { return itemPath; }
//  public void setSysKey( int sysKey ) { this.sysKey = sysKey; }

    public String getName() { return name; }
//  public void setName( String name ) { this.name = name; }

    public String getType() { return type; }
//  public void setType( String type ) { this.type = type; }

    public Path getPath() { return binding; }

    public DynamicTreeBuilder getTreeBuilder() { return loader; }

    @Override
	public String toString() {
        if (this.name.length() > 0) {
            return this.name;
        }
        else { return "Cristal"; }
    }

    public Icon getIcon() {
        if (icon != null) return icon;
        return(isExpandable?folder:emptyLeaf);
    }

    public String getIconName() {
        return iconName;
    }

    public void setIcon(String icon) {
        iconName = icon;
        this.icon = ImageLoader.findImage("typeicons/"+icon+"_16.png");
        if (this.icon==ImageLoader.nullImg) this.icon = ImageLoader.findImage("typeicons/item_16.png");
    }

	public JPopupMenu getPopupMenu() {
		JPopupMenu popup = new JPopupMenu();
		JMenuItem menuItem = new JMenuItem("Refresh");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isExpandable) refresh();
			}
		});
		popup.add(menuItem);
		return popup;
	}

	public String getToolTip() {
   		if (toolTip != null && toolTip.length()>0)
   			return toolTip;
   		else
   			return type;
	}

	public void setToolTip(String tip) {
		this.toolTip = tip;
	}}
