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
package org.cristalise.gui.tabs;

import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

import java.util.StringTokenizer;

import javax.swing.JTabbedPane;

import org.cristalise.gui.ItemDetails;
import org.cristalise.gui.tabs.collection.AggregationView;
import org.cristalise.gui.tabs.collection.CollectionView;
import org.cristalise.gui.tabs.collection.DependencyView;
import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("serial")
@Slf4j
public class CollectionPane extends ItemTabPane {
    JTabbedPane collTabs;

    public CollectionPane() {
        super("Collection", "Item Collection");
        createLayout();
    }
    
    @Override
    public void setParent(ItemDetails parent) {
        super.setParent(parent);

        Vertx vertx = Gateway.getVertx();
        vertx.eventBus().localConsumer(parent.getItemPath().getUUID() + "/" + COLLECTION, message -> {
            String[] tokens = ((String) message.body()).split(":");
             String collPath = tokens[0];

            if (tokens[1].equals("DELETE")) return;

            vertx.executeBlocking(promise -> {
                try {
                    int idx = collPath.lastIndexOf("/");
                    String collName = collPath.substring(0, idx);

                    if (collPath.endsWith("/last")) {
                        add(sourceItem.getItem().getCollection(collName));
                    }
                    else {
                        Integer version = new Integer(collPath.substring(idx+1));
                        add(sourceItem.getItem().getCollection(collName, version));
                    }
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

    public void add(Collection<? extends CollectionMember> contents) {
        log.debug("Got " + contents.getName() + ": " + contents.getClass().getName());
        log.debug("Looking for existing " + contents.getName());
        CollectionView<? extends CollectionMember> thisCollView = findTabForCollName(contents.getName());
        if (contents instanceof Aggregation) {
            AggregationView thisAggView;
            if (thisCollView == null) {
                thisAggView = new AggregationView();
                thisAggView.setItem(sourceItem.getItem());
                thisCollView = thisAggView;
            }
            else {
                thisAggView = (AggregationView) thisCollView;
            }
            thisAggView.setCollection((Aggregation) contents);

        }
        else if (contents instanceof Dependency) {
            DependencyView thisDepView;
            if (thisCollView == null) {
                thisDepView = new DependencyView();
                thisDepView.setItem(sourceItem.getItem());
                thisCollView = thisDepView;
            }
            else {
                thisDepView = (DependencyView) thisCollView;
            }
            thisDepView.setCollection((Dependency) contents);

        }
        else {
            log.error("Collection type " + contents.getClass().getName() + " not known");
            return;
        }
        log.info("Adding new " + thisCollView.getClass().getName());
        collTabs.add(contents.getName() + (contents instanceof CollectionDescription ? "*" : ""), thisCollView);
    }

    @SuppressWarnings("unchecked")
    private CollectionView<? extends CollectionMember> findTabForCollName(String collName) {
        CollectionView<? extends CollectionMember> thisCollView = null;
        for (int i = 0; i < collTabs.getTabCount(); i++) {
            thisCollView = (CollectionView<? extends CollectionMember>) collTabs.getComponentAt(i);
            if (thisCollView.getCollection() != null && collName.equals(thisCollView.getCollection().getName()))
                return thisCollView;
        }
        return null;
    }

    protected void createLayout() {
        initPanel();
        // Add the collection tab pane
        collTabs = new JTabbedPane();
        add(collTabs);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Collection Loader");
        try {
            String collNames = sourceItem.getItem().queryData(ClusterType.COLLECTION + "/all");
            StringTokenizer tok = new StringTokenizer(collNames, ",");
            while (tok.hasMoreTokens()) {
                add(sourceItem.getItem().getCollection(tok.nextToken()));
            }
        }
        catch (Exception e) {
            log.error("Error loading collections", e);
        }
    }

    @Override
    public void reload() {
        Gateway.getStorage().clearCache(sourceItem.getItemPath(), ClusterType.COLLECTION.getName());
        collTabs.removeAll();
        initForItem(sourceItem);
    }
}
