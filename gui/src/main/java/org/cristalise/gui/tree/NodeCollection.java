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

import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;

import org.cristalise.gui.ItemTabManager;
import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.Logger;


public class NodeCollection extends Node implements ProxyObserver<Collection<? extends CollectionMember>> {

    ItemProxy parent;
    Collection<? extends CollectionMember> thisCollection;
    String path;

    public NodeCollection(ItemProxy parent, String name, ItemTabManager desktop) {
    	super(desktop);
        this.parent = parent;
        this.name = name;
        this.path = parent.getPath()+"/"+ClusterType.COLLECTION+"/"+name+"/last";
        createTreeNode();
        this.makeExpandable();
    }
    
    public NodeCollection(ItemProxy parent, Collection<? extends CollectionMember> coll, ItemTabManager desktop) {
    	super(desktop);
        this.parent = parent;
        this.name = coll.getName();
        this.path = parent.getPath()+"/"+ClusterType.COLLECTION+"/"+name+"/last";
        createTreeNode();
        this.makeExpandable();
        add(coll);
    }

    @Override
	public void loadChildren() {
        Logger.msg(8, "NodeCollection::loadChildren()");
        try {
        	if (thisCollection == null) {
        		Collection<? extends CollectionMember> initColl = (Collection<? extends CollectionMember>)parent.getObject(ClusterType.COLLECTION+"/"+name+"/last");
        		add(initColl);
        	}
            parent.subscribe(new MemberSubscription<Collection<? extends CollectionMember>>(this, ClusterType.COLLECTION.getName(), false));
        } catch (ObjectNotFoundException ex) {
            end(false);
            return;
        }
    }
    
    @Override
    public void add(Collection<? extends CollectionMember> contents) {
    	if (!contents.getName().equals(name)) return;
    	this.type = contents.getClass().getSimpleName();
        ArrayList<? extends CollectionMember> newMembers = contents.getMembers().list;
        ArrayList<? extends CollectionMember> oldMembers;
        if (thisCollection == null)
        	oldMembers = new ArrayList<CollectionMember>();
        else
        	oldMembers = thisCollection.getMembers().list;
        
        ArrayList<Path> currentPaths = new ArrayList<Path>();
        // add any missing paths
        for (CollectionMember newMember : newMembers) {
        	ItemPath itemPath = newMember.getItemPath();
            if (!oldMembers.contains(newMember) && itemPath != null) {
                currentPaths.add(itemPath);
                NodeItem newMemberNode = new NodeItem(itemPath, desktop);
                newMemberNode.setCollection(contents, newMember.getID(), parent);
                newMemberNode.setToolTip(getPropertyToolTip(newMember.getProperties()));
                add(newMemberNode);
            }
        }
        // remove those no longer present
        for (Path childPath : childNodes.keySet()) {
        	if (!currentPaths.contains(childPath)) {
        		remove(childPath);
        	}
			
		}
        
        thisCollection = contents;
    	if (isDependency())
    		setToolTip(getPropertyToolTip(((Dependency)contents).getProperties()));
    	end(false);
    }
    
    public boolean addMember(ItemPath itemPath) {
    	if (!isDependency()) return false;
    	String[] params = { thisCollection.getName(), itemPath.getUUID().toString() };
		try {
			MainFrame.userAgent.execute(parent, "AddMemberToCollection", params);
			return true;
		} catch (Exception e1) {
			MainFrame.exceptionDialog(e1);
			return false;
		}
    }
    
    public static String getPropertyToolTip(CastorHashMap props) {
    	if (props.size() == 0) return null;
    	StringBuffer verStr = new StringBuffer("<html>");
    	for (KeyValuePair prop : props.getKeyValuePairs()) {
			verStr.append("<b>").append(prop.getKey()).append(":</b> ").append(prop.getValue()).append("<br/>");
		}
    	return verStr.append("</html>").toString();
    }

    @Override
	public DefaultMutableTreeNode getTreeNode() {
        return treeNode;
    }



	@Override
	public void remove(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void control(String control, String msg) {
		// TODO Auto-generated method stub
		
	}

	public boolean isDependency() {
		return thisCollection instanceof Dependency;
	}
}
