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

import java.awt.HeadlessException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Language;
import org.cristalise.kernel.utils.Logger;


public class CollectionHistoryWindow extends JFrame  {

	JTable historyTable;
	HistoryTableModel historyModel;

	public CollectionHistoryWindow(ItemProxy item, Aggregation coll) throws HeadlessException {
		super("Collection History");
		historyModel = new HistoryTableModel(item, coll);
		historyTable = new JTable(historyModel);
		this.getContentPane().add(new JScrollPane(historyTable));
		historyTable.addMouseListener(new HistoryTableListener(item));
		this.pack();
        super.toFront();
        this.validate();
        this.setVisible(true);
	}

	private class HistoryTableModel extends AbstractTableModel implements ProxyObserver<Event> {

		ItemProxy item;
		ArrayList<Event> collEvents;
		ArrayList<Object> collEventData;
		Aggregation coll;
		public HistoryTableModel(ItemProxy item, Aggregation coll) {
			this.item = item;
			this.coll = coll;
			collEvents = new ArrayList<Event>();
			collEventData = new ArrayList<Object>();
			item.subscribe(new MemberSubscription<Event>(this, ClusterStorage.HISTORY, true));
		}
		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch(columnIndex) {
				case 0: return Language.translate("Date");
				case 1: return Language.translate("Operation");
				case 2: return Language.translate("Slot");
				case 3: return Language.translate("Child");
				default: return "";
			}
		}
		@Override
		public int getRowCount() {
			return collEvents.size();
		}
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Event ev = collEvents.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return ev.getTimeString();
			case 1:
				if (ev.getStepName().equals("AssignItemToSlot"))
					return "Item Assigned";
				else
					return "Collection replaced";
			case 2:
				if (ev.getStepName().equals("AssignItemToSlot"))
					return ((String[])collEventData.get(rowIndex))[1];
				return "";
			case 3:
				if (ev.getStepName().equals("AddC2KObject"))
					return "Click to view";
				String name;
					try {
						ItemProxy childItem = Gateway.getProxyManager().getProxy(new ItemPath(((String[])collEventData.get(rowIndex))[2]));
						name = childItem.getName();
					} catch (ObjectNotFoundException e) {
						name = "Item deleted: "+((String[])collEventData.get(rowIndex))[2];
					} catch (Exception e) {
						name = "Problem resolving Item key: "+((String[])collEventData.get(rowIndex))[2];
					}
				return name;
			default:
				return "";
			}
		}
		public Object getEventData(int row) {
			return collEventData.get(row);
		}
		@Override
		public void add(Event thisEv) {
			if (thisEv.getStepName().equals("AssignItemToSlot") || thisEv.getStepName().equals("AddC2KObject")) {
				String[] params;
				try {
				Outcome oc = (Outcome)item.getObject(ClusterStorage.OUTCOME+"/PredefinedStepOutcome/0/"+thisEv.getID());
				params = PredefinedStep.getDataList(oc.getData());
				} catch (ObjectNotFoundException ex) { return; }
				if (thisEv.getStepName().equals("AssignItemToSlot")) {
					if (params[0].equals(coll.getName()))
						collEventData.add(params);
					else return;
				}
				else {
					Object obj;
					try {
						obj = Gateway.getMarshaller().unmarshall(params[0]);
					} catch (Exception e) {
						Logger.error(e);
						return;
					}
					if (obj instanceof Collection)
						collEventData.add(obj);
					else return;

				}
			}
			else return;
			collEvents.add(thisEv);
			fireTableRowsInserted(collEvents.size()-1, collEvents.size()-1);
		}
		@Override
		public void remove(String id) { }
		@Override
		public void control(String control, String msg) {
		}
	}

	private class HistoryTableListener extends MouseAdapter {

		ItemProxy item;
		public HistoryTableListener(ItemProxy item) {
			this.item = item;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount()==2) {
				int row = historyTable.getSelectedRow();
				Object data = historyModel.getEventData(row);
				if (data instanceof Aggregation) {
					showColl((Aggregation)data);
				}
				else {
					String[] params = (String[])data;
					try {
					ItemProxy childItem = Gateway.getProxyManager().getProxy(new ItemPath(params[2]));
					MainFrame.itemFinder.pushNewKey(childItem.getName());
					} catch (Exception ex) { }
				}
			}
		}
		public void showColl(Aggregation coll) {
			JFrame newFrame = new JFrame();
			AggregationView newView = new AggregationView();
			newView.setCollection(coll);
			newView.setItem(item);
			newFrame.getContentPane().add(newView);
			newFrame.pack();
			newFrame.toFront();
			newFrame.validate();
			newFrame.setVisible(true);
		}
	}
}
