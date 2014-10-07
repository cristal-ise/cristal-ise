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
package org.cristalise.gui.tabs.collection;
import java.awt.GridLayout;

import javax.swing.JSplitPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.TreeBrowser;
import org.cristalise.gui.tree.Node;
import org.cristalise.gui.tree.NodeCollection;
import org.cristalise.gui.tree.NodeItem;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.ObjectNotFoundException;

/**
 * @version $Revision: 1.2 $ $Date: 2005/06/02 12:17:22 $
 * @author $Author: abranson $
 */
public class DependencyView extends CollectionView<DependencyMember>
{
	TreeBrowser tree;
	CollectionMemberPropertyPanel propPanel;
	JSplitPane split;
	
	public DependencyView()
	{
		super();
        setLayout(new GridLayout(1, 1));
        createLayout();
	}

	@Override
	public void setCollection(Collection<DependencyMember> contents)
	{
        thisColl = contents;
        NodeCollection collNode = new NodeCollection(item, thisColl.getName(), null);
		tree = new TreeBrowser(MainFrame.myDesktopManager, collNode);
		tree.getTree().addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getPath() == null)  {
					propPanel.clear();
				}
				else {
					Node selectedNode = (Node)((DefaultMutableTreeNode)e.getPath().getLastPathComponent()).getUserObject();
					if (selectedNode instanceof NodeItem) {
						NodeItem thisItem = (NodeItem)selectedNode;
						if (thisItem.getParentCollection() != null) {
							try {
								propPanel.setMember(thisItem.getParentCollection().getMember(thisItem.getSlotNo()));
								return;
							} catch (ObjectNotFoundException e1) { }
						}
						propPanel.clear();
					}
				}
			}
		});
		split.setLeftComponent(tree);
	}
	public void createLayout()
	{
		propPanel = new CollectionMemberPropertyPanel();
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setRightComponent(propPanel);
	    add(split);
	    
	}
	
}
