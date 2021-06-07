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

import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;

import org.cristalise.gui.ItemTabManager;
import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.instance.predefined.AddMembersToCollection;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeCollection extends Node {

    ItemProxy                              parent;
    Collection<? extends CollectionMember> thisCollection;
    String                                 path;

    public NodeCollection(ItemProxy parent, String name, ItemTabManager desktop) {
        super(desktop);
        this.parent = parent;
        this.name = name;
        this.path = parent.getPath() + "/" + COLLECTION + "/" + name + "/last";
        createTreeNode();
        this.makeExpandable();

        Vertx vertx = Gateway.getVertx();
        vertx.eventBus().localConsumer(parent.getPath().getUUID() + "/" + COLLECTION, message -> {
            String[] tokens = ((String) message.body()).split(":");
             String collPath = tokens[0];

            if (tokens[1].equals("DELETE")) return;

            vertx.executeBlocking(promise -> {
                try {
                    add(parent.getCollection(collPath));
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

    public NodeCollection(ItemProxy parent, Collection<? extends CollectionMember> coll, ItemTabManager desktop) {
        this(parent, coll.getName(), desktop);
        add(coll);
    }

    @Override
    public void loadChildren() {
        try {
            if (thisCollection == null) {
                @SuppressWarnings("unchecked")
                Collection<? extends CollectionMember> initColl = (Collection<? extends CollectionMember>) parent
                        .getObject(COLLECTION + "/" + name + "/last");
                add(initColl);
            }
        }
        catch (ObjectNotFoundException ex) {
            end(false);
            return;
        }
    }

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
            setToolTip(getPropertyToolTip(((Dependency) contents).getProperties()));
        end(false);
    }

    public boolean addMember(ItemPath itemPath) {
        if (!isDependency()) return false;
        try {
            Dependency dep = new Dependency(thisCollection.getName());
            CastorHashMap memberProps1 = new CastorHashMap();
            dep.addMember(itemPath, memberProps1, "", null);
            MainFrame.userAgent.execute(parent, AddMembersToCollection.class, Gateway.getMarshaller().marshall(dep));
            return true;
        }
        catch (Exception e1) {
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

    public boolean isDependency() {
        return thisCollection instanceof Dependency;
    }
}
