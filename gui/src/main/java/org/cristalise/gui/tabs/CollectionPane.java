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
import java.util.StringTokenizer;

import javax.swing.JTabbedPane;

import org.cristalise.gui.tabs.collection.AggregationView;
import org.cristalise.gui.tabs.collection.CollectionView;
import org.cristalise.gui.tabs.collection.DependencyView;
import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

/**
 * @version $Revision: 1.36 $ $Date: 2005/10/06 06:51:15 $
 * @author $Author: abranson $
 */
public class CollectionPane extends ItemTabPane implements ProxyObserver<Collection<? extends CollectionMember>>
{
    JTabbedPane collTabs;

    public CollectionPane()
	{
		super("Collection", "Item Collection");
		createLayout();
	}

    @Override
	public void add(Collection<? extends CollectionMember> contents)
	{
        Logger.msg(5, "Got "+contents.getName()+": "+contents.getClass().getName());
        Logger.msg(7, "Looking for existing "+contents.getName());
		CollectionView<? extends CollectionMember> thisCollView = findTabForCollName(contents.getName());
        if (contents instanceof Aggregation) {
        	AggregationView thisAggView;
        	if (thisCollView == null) {
        		thisAggView = new AggregationView();
        		thisAggView.setItem(sourceItem.getItem());
        		thisCollView = thisAggView;
        	}
        	else {
        		thisAggView = (AggregationView)thisCollView;
        	}
            thisAggView.setCollection((Aggregation)contents);
            
        }
        else if (contents instanceof Dependency) {
        	DependencyView thisDepView;
        	if (thisCollView == null) {
        		thisDepView = new DependencyView();
        		thisDepView.setItem(sourceItem.getItem());
                thisCollView = thisDepView;            		
        	}
        	else {
        		thisDepView = (DependencyView)thisCollView;
        	}
        	thisDepView.setCollection((Dependency)contents);

        }
        else {
            Logger.error("Collection type "+contents.getClass().getName()+" not known");
            return;
        }
        Logger.msg(3, "Adding new "+thisCollView.getClass().getName());
        collTabs.add(contents.getName()+(contents instanceof CollectionDescription?"*":""), thisCollView);
	}

    @Override
	public void remove(String id)
	{

	}

    private CollectionView<? extends CollectionMember> findTabForCollName(String collName) {
        CollectionView<? extends CollectionMember> thisCollView = null;
        for (int i = 0; i < collTabs.getTabCount(); i++) {
        	thisCollView = (CollectionView<? extends CollectionMember>)collTabs.getComponentAt(i);
        	if (thisCollView.getCollection() != null && collName.equals(thisCollView.getCollection().getName()))
        			return thisCollView;
        }
        return null;
    }

    protected void createLayout()
	{
		initPanel();
		// Add the collection tab pane
		collTabs = new JTabbedPane();
		add(collTabs);
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName("Collection Loader");
		sourceItem.getItem().subscribe(new MemberSubscription<Collection<?>>(this, ClusterStorage.COLLECTION, false));
        try {
            String collNames = sourceItem.getItem().queryData(ClusterStorage.COLLECTION+"/all");
            StringTokenizer tok = new StringTokenizer(collNames, ",");
            while (tok.hasMoreTokens()) {
                Collection<?> thisLastColl = (Collection<?>) sourceItem.getItem().getObject(ClusterStorage.COLLECTION+"/"+tok.nextToken()+"/last");
                add(thisLastColl);
            }
        } catch (Exception e) {
        	Logger.error(e);
            Logger.msg(2, "Error loading collections");
        }
	}

	@Override
	public void reload()
	{
		Gateway.getStorage().clearCache(sourceItem.getItemPath(), ClusterStorage.COLLECTION);
        collTabs.removeAll();
		initForItem(sourceItem);
	}

	@Override
	public void control(String control, String msg) {
	}
}
