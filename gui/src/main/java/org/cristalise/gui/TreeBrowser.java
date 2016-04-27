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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.cristalise.gui.tree.Node;
import org.cristalise.gui.tree.NodeItem;
import org.cristalise.gui.tree.NodeTransferHandler;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.utils.Logger;


/**
 * Container for the tree browser
 * @version $Revision: 1.31 $ $Date: 2006/01/17 07:49:44 $
 * @author  $Author: abranson $
 */

 // must put in top level list of loaded items, so we don't have duplicates
public class TreeBrowser extends JPanel implements DomainKeyConsumer
{
    private ItemTabManager desktop;
    protected JTree tree;
    private Node userRoot;

    public TreeBrowser(ItemTabManager target, Node userRoot) {
        setLayout(new java.awt.BorderLayout());
        //record the desktop and node factory for our item frames
        this.desktop = target;
        this.userRoot = userRoot;
        this.setPreferredSize(new Dimension(300, 500));
        tree = new JTree(new DefaultTreeModel(userRoot.getTreeNode()));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setToggleClickCount(3); // need three clicks to expand a branch
        tree.addTreeExpansionListener(
            new TreeExpansionListener() {
                @Override
				public void treeCollapsed(TreeExpansionEvent e) {
                    //REVISIT: possible reaping here if things are getting heavy
                }
                @Override
				public void treeExpanded(TreeExpansionEvent e) {
                    TreePath p = e.getPath();
                    // find the clicked tree node
                    DefaultMutableTreeNode nodeClicked = (DefaultMutableTreeNode)p.getLastPathComponent();
                    // run the tree builder if it is there.
                    DefaultMutableTreeNode loadNode = (DefaultMutableTreeNode)nodeClicked.getFirstChild();
                    if (loadNode.getUserObject() instanceof DynamicTreeBuilder) {
                        DynamicTreeBuilder loading = (DynamicTreeBuilder)loadNode.getUserObject();
                        if (loading.state == DynamicTreeBuilder.IDLE) {
                            loading.buildInfo(tree);
                            loading.start();
                        }
                    }
                }
            }
        );

        //Enable tool tips.
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.setCellRenderer(new ItemRenderer());
        tree.addMouseListener(new TreeMouseListener());
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON);
        tree.setTransferHandler(new NodeTransferHandler(this));
        JScrollPane myScrollPane = new JScrollPane(tree);
        this.add(myScrollPane);
        DefaultMutableTreeNode loadNode = (DefaultMutableTreeNode)userRoot.getTreeNode().getFirstChild();
        DynamicTreeBuilder loading = (DynamicTreeBuilder)loadNode.getUserObject();
        loading.buildInfo(tree);
        loading.start();
    }

    @Override
	public void push(DomainPath target) {
        String[] components = target.getPath();
        Node currentNode = userRoot;
        Object[] treePath = new Object[components.length+1];
        treePath[0] = currentNode.getTreeNode();
        for (int i=0; i<components.length; i++) {
            // create sub-path
            String[] newPath = new String[i+1];
            for (int j=0; j<newPath.length; j++)
                newPath[j] = components[j];
            DomainPath nextNodePath = new DomainPath(newPath);
            Node nextNode = currentNode.getChildNode(nextNodePath);
            if (nextNode == null) {
                Logger.msg(6, "TreeBrowser.push() - creating "+nextNodePath);
                nextNode = currentNode.newNode(nextNodePath);
                currentNode.add(nextNode);
                DynamicTreeBuilder builder = currentNode.getTreeBuilder();
                builder.buildInfo(tree);
                builder.add(nextNode);
            }
            treePath[i+1] = nextNode.getTreeNode();
            currentNode = nextNode;
        }
        // select it
        TreePath targetNode = new TreePath(treePath);
        if (Logger.doLog(5)) dumpPath(targetNode, 5);

        tree.clearSelection();
        tree.addSelectionPath(targetNode);
        tree.makeVisible(targetNode);
        // open it
        if (currentNode instanceof NodeItem) {
            desktop.add((NodeItem)currentNode);
        }
    }

    public JTree getTree() {
		return tree;
	}

	@Override
	public void push(String name) {
    	// only interested in real paths
        JOptionPane.showMessageDialog(null, "'"+name+"' was not found.",
            "No results", JOptionPane.INFORMATION_MESSAGE);

    }
    
    public Node getSelectedNode() {
    	Object selObj = tree.getLastSelectedPathComponent();
    	if (selObj != null)
            try {
                DefaultMutableTreeNode nodeClicked = (DefaultMutableTreeNode)selObj;
                Object userObject = nodeClicked.getUserObject();
                if (userObject instanceof Node) return (Node)userObject;
            } catch (Exception ex) { } // Not a node that was clicked on
    	return null;
    }
    
    public Node getNodeAt(Point p) {
        TreePath selPath = tree.getPathForLocation(p.x, p.y);
        if (selPath != null)
            try {
                DefaultMutableTreeNode nodeClicked = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                Object userObject = nodeClicked.getUserObject();
                if (userObject instanceof Node) return (Node)userObject;
            }
        catch (Exception ex) { } // Not a node that was clicked on
        return null;
    }

    private static void dumpPath(TreePath selPath, int logLevel) {
        if (selPath == null) { Logger.msg(logLevel, "TreeBrowser.dumpPath() - selPath null"); return; }
        for (int i =0; i<selPath.getPath().length; i++)
            Logger.msg(logLevel, "TreeBrowser.dumpPath() - selPath "+i+" = "+selPath.getPath()[i]);
    }

    private class ItemRenderer extends DefaultTreeCellRenderer {
        public ItemRenderer() {
        }

        @Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
            int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                Object thisLeaf = ((DefaultMutableTreeNode)value).getUserObject();
                if (thisLeaf instanceof DynamicTreeBuilder) {
                    DynamicTreeBuilder thisLoader = (DynamicTreeBuilder)thisLeaf;
                    ImageIcon loadGif = thisLoader.getIcon();
                    setIcon(loadGif);
                    loadGif.setImageObserver(tree);
                    setToolTipText("Tree Content Loader");
                }
                else if (thisLeaf instanceof Node) {
                    Node thisNode = (Node)thisLeaf;
                    if (thisNode.getIcon() !=null) setIcon(thisNode.getIcon());
                    setToolTipText(thisNode.getToolTip());
                }
                return this;
        }
    }

    private class TreeMouseListener extends MouseAdapter {
    	@Override
		public void mousePressed(MouseEvent e) {
    		if (e.isPopupTrigger())
    			showPopup(e);
    		else {
    			Object source = getNodeAt(e.getPoint());
                if (source == null) return;
    			if (e.getClickCount() == 2) {
                    if (source instanceof NodeItem) {
                    	NodeItem thisNode = (NodeItem)source;
    	                desktop.add(thisNode);
            	    }
                    if (source instanceof DynamicTreeBuilder) {
                        DynamicTreeBuilder thisLoader = (DynamicTreeBuilder)source;
                        if (thisLoader.state == DynamicTreeBuilder.PARTIAL)
                            thisLoader.start();
                    }
                }
    		}
    	}
        @Override
		public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
            	showPopup(e);
            }
        }
        private void showPopup(MouseEvent e) {
        	Object source = getNodeAt(e.getPoint());
            if (source == null) return;
        	if (source instanceof Node) {
               	Node thisNode = (Node)source;
	           	thisNode.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
        	}
        }
    }
}
