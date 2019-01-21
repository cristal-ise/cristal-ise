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

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.cristalise.gui.tree.Node;
import org.cristalise.gui.tree.NodeItem;
import org.cristalise.gui.tree.NodeSubscriber;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.utils.Logger;


/**
 * Installed as the user object on a single child node of a new node known to be composite.
 * <p>Shows 'Loading . . .' when the branch is opened, but a TreeExpansionListener attempts to fire this thread off in the first child node.
 * <br>When started, this thread will retrieve all of the real child nodes and add them to its parent while removing itself (hopefully for garbage collection)
 *
 * @version $Revision: 1.24 $ $Date: 2004/12/15 12:12:06 $
 * @author  $Author: abranson $
 */

public class DynamicTreeBuilder extends Node implements NodeSubscriber {
    private DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode parent;
    public short state = IDLE;
    public static final short IDLE = 0;
    public static final short LOADING = 1;
    public static final short PARTIAL = 2;
    public static final short FINISHED = 3;
    private final DefaultMutableTreeNode loading;
    private static ImageIcon loadIcon = ImageLoader.findImage("loading.gif");
    private static ImageIcon pauseIcon = ImageLoader.findImage("reload.gif");

    /**
     * The newly created DynamicTreeBuilder records its parent node - the one for which it will build child nodes for.
     * @param nodeClicked The Parent Tree Node that will be populated.
     * @see NodeItem
     * @see TreeDisplay*/
    public DynamicTreeBuilder(DefaultMutableTreeNode parent, ItemTabManager desktop) {
    	super(desktop);
        this.parent = parent;
        loading = new DefaultMutableTreeNode(this);
    }

    /**
     * Before the tree builder can execute, it needs to be given references to the tree and NodeFactory needed to create the new nodes.
     * @param myNodeFactory The NodeFactory that can be queried for new NodeItems.
     * @param parentTree The JTree in which this node is currently contained.
     */
    public void buildInfo(JTree parentTree) {
        this.treeModel = (DefaultTreeModel)parentTree.getModel();
    }

    public void start() {
        // find the clicked tree node
        Node parentNode = (Node)parent.getUserObject();
        Logger.msg(2, "DynamicTreeBuilder.start() - Filling in children of '"+parentNode.toString()+"'");
        if (state == IDLE)
            parentNode.subscribeNode(this);
        else
            parentNode.loadMore();
        state = LOADING;
    }

    /**
     * Used by the JTree to find the text representation of the node.
     */
    @Override
	public String toString() {
        switch (state) {
            case IDLE:
                return "Initializing Tree Node Loader";
            case LOADING:
                return "Loading . . .";
            case PARTIAL:
                return "Double-click to load more";
            case FINISHED:
                return "Done";
            default:
                return "";
        }

    }

    @Override
	public ImageIcon getIcon() {
        if (state == LOADING)
            return loadIcon;
        else
            return pauseIcon;
    }

    @Override
	public DefaultMutableTreeNode getTreeNode() {
        return loading;
    }

    @Override
	public void add(Node newNode) {
        Logger.msg(2, "DynamicTreeBuilder.add() - Received item for tree. Name: "+newNode);

        // have we inserted the node yet?
        SwingUtilities.invokeLater(new TreeAddThread(newNode));
    }

    class TreeAddThread implements Runnable {
        Node newNode;
        TreeAddThread(Node newNode) {
            this.newNode = newNode;
        }
        @Override
		public void run() {
            boolean inserted = false;
            DefaultMutableTreeNode newTreeNode = newNode.getTreeNode();
            // loop though all children unless we have done the insertion
            for (int i=0; i<parent.getChildCount() && !inserted; i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode)treeModel.getChild(parent, i);
                if (child == loading)  continue; // skip loading node

                Node childNode = (Node)child.getUserObject();
                if (childNode.getName().equals(newNode.getName())) {
                    // we already have this one, skip it
                    inserted = true;
                    break;
                    }
                if (childNode.getName().compareTo(newNode.getName()) >= 0) {
                    // if the next string is 'greater than' ours, insert the node before
                    treeModel.insertNodeInto(newTreeNode, parent, i);
                    inserted = true;
                    break;
                }

            }
            // if we haven't inserted yet, it must go at the end.

            if (!inserted)
                treeModel.insertNodeInto(newTreeNode, parent, parent.getChildCount());
        }

    }

    class TreeRemoveThread implements Runnable {
        DefaultMutableTreeNode oldNode;
        TreeRemoveThread(DefaultMutableTreeNode oldNode) {
            this.oldNode = oldNode;
        }

        @Override
		public void run() {
            treeModel.removeNodeFromParent(oldNode);
        }
    }

    @Override
	public void end(boolean more) {
        if (more) {
            state = PARTIAL;
        }
        else {
            state = FINISHED;
            synchronized(treeModel) {
        	   if (loading.getParent() != null)
    	            SwingUtilities.invokeLater(new TreeRemoveThread(loading));
            }
        }
    }

    @Override
	public void remove(Path path) {
        synchronized (treeModel) {
            for (int i=0; i<parent.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode)treeModel.getChild(parent, i);
                if (!(child.getUserObject() instanceof Node)) continue;
                Node childNode = (Node)child.getUserObject();
                if (childNode.getPath().equals(path)) {
                    SwingUtilities.invokeLater(new TreeRemoveThread(child));
                    return;
                }
            }
        }
    }

	@Override
	public void loadChildren() {
		// No children in the loader
		
	}
}
