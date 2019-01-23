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
/*
 * StatusPane.java
 *
 * Created on March 20, 2001, 3:30 PM
 */

package org.cristalise.gui.tabs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;


/**
 * Pane to display all work orders that this agent can execute, and activate
 * them on request from the user. Subscribes to NodeItem for WorkOrder objects.
 * @version $Revision: 1.22 $ $Date: 2005/04/26 06:48:13 $
 * @author  $Author: abranson $
 */
public class HistoryPane extends ItemTabPane implements ActionListener, ProxyObserver<Event> {

    History history;
    HistoryTableModel model;
    JTable eventTable;
	JButton startButton = new JButton("<<");
    JButton prevButton = new JButton("<");
	JButton nextButton = new JButton(">");
	JButton endButton = new JButton(">>");
    public static final int SIZE = 30;
    int currentSize = SIZE;

    public HistoryPane() {
        super("History", "Event History");
        initPanel();

		// add buttons
        Box navBox = Box.createHorizontalBox();
        navBox.add(startButton); navBox.add(prevButton);
		navBox.add(nextButton); navBox.add(endButton);

        // setup buttons
		//startButton.setEnabled(false); nextButton.setEnabled(false);
		//prevButton.setEnabled(false); endButton.setEnabled(false);
		startButton.setActionCommand("start");
		startButton.addActionListener(this);
		prevButton.setActionCommand("prev");
		prevButton.addActionListener(this);
		nextButton.setActionCommand("next");
		nextButton.addActionListener(this);
		endButton.setActionCommand("end");
		endButton.addActionListener(this);
		add(navBox);

		// Create table
		eventTable = new JTable();
		JScrollPane eventScroll= new JScrollPane(eventTable);
		add(eventScroll);
		
		// open viewpoint pane when outcome events are clicked on
		eventTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					Event selected = model.getEventForRow(eventTable.getSelectedRow());
					if (selected.getSchemaName()!=null && selected.getViewName()!=null)
						parent.runCommand("Data Viewer", selected.getSchemaName()+":"+selected.getViewName());
				}
			}
		});

    }

    @Override
	public void reload() {
        history.clear();
        jumpToEnd();
    }

    @Override
	public void run() {
        Thread.currentThread().setName("History Pane Builder");
        MainFrame.progress.startBouncing("Loading history");
		try {
			history = (History)sourceItem.getItem().getObject(ClusterStorage.HISTORY);
			history.activate();
            sourceItem.getItem().subscribe(new MemberSubscription<Event>(this, ClusterStorage.HISTORY, false));
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
		}
		model = new HistoryTableModel();
		eventTable.setModel(model);
		jumpToEnd();
    }

    public void jumpToEnd() {
		int lastEvent = history.getLastId();
		int firstEvent = 0; currentSize = SIZE;
		if (lastEvent > currentSize) firstEvent = lastEvent - currentSize + 1;
		if (lastEvent < currentSize) currentSize = lastEvent + 1;
		Logger.msg(5, "HistoryPane.run() - init table start "+firstEvent+" for "+currentSize);
		model.setView(firstEvent, currentSize);
		MainFrame.progress.stopBouncing("History loaded");
    }

    @Override
	public void add(Event contents) {
        jumpToEnd();
    }

    @Override
	public void remove(String id) {
        // don't have to deal with this normally
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("end")) {
			jumpToEnd();
			return;
		}

		int lastEvent = history.getLastId();
		int startEvent = model.getStartId();
		if (e.getActionCommand().equals("start")) {
			currentSize = SIZE;
			startEvent = 0;
		}

		else if (e.getActionCommand().equals("prev")) {
			currentSize = SIZE;
			startEvent-=currentSize;
			if (startEvent<0) startEvent = 0;
		}
		else if (e.getActionCommand().equals("next")) {
			currentSize = SIZE;
			startEvent+=currentSize;
			if (startEvent > lastEvent)
				startEvent = lastEvent - currentSize +1;
		}
		else { // unknown action
			return;
		}

		model.setView(startEvent, currentSize);
	}

    private class HistoryTableModel extends AbstractTableModel {
    	Event[] event;
    	StateMachine[] sm;
    	Integer[] ids;
    	int loaded = 0;
    	int startId = 0;

    	public HistoryTableModel() {
    		event = new Event[0];
    		ids = new Integer[0];
    	}

    	public int getStartId() {
    		return startId;
    	}

		public void setView(int startId, int size) {
			event = new Event[size];
			ids = new Integer[size];
			sm = new StateMachine[size];
			this.startId = startId;
			for (int i=0; i<size; i++) {
				event[i] = history.getEvent(startId+i);
				try {
					sm[i] = LocalObjectLoader.getStateMachine(event[i].getStateMachineName(), event[i].getStateMachineVersion());
				} catch (Exception ex) { Logger.error(ex); }
				ids[i] = new Integer(startId+i);
				loaded = i+1;
			}
			fireTableStructureChanged();
		}
		/**
		 * @see javax.swing.table.TableModel#getColumnClass(int)
		 */
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch(columnIndex) {
				case 0:
					return Integer.class;
				default:
					return String.class;
			}
		}

		/**
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			return 8;
		}

		/**
		 * @see javax.swing.table.TableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int columnIndex) {
			switch(columnIndex) {
				case 0: return "ID";
				case 1: return "Activity";
				case 2: return "Transition";
				case 3: return "Date";
                case 4: return "Agent Name";
                case 5: return "Agent Role";
                case 6: return "Schema";
                case 7: return "View";
				default: return "";
			}
		}

		/**
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		@Override
		public int getRowCount() {
			return loaded;
		}

		/**
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (event.length <= rowIndex || event[rowIndex] == null)
				return "";
			try {
				switch (columnIndex) {
					case 0: return ids[rowIndex];
					case 1: return event[rowIndex].getStepName();
					case 2: return sm[rowIndex]==null?"Unknown":
						sm[rowIndex].getTransition(event[rowIndex].getTransition()).getName();
					case 3: return event[rowIndex].getTimeString();
                    case 4: return event[rowIndex].getAgentPath().getAgentName();
                    case 5: return event[rowIndex].getAgentRole();
                    case 6:
                    	String schId = event[rowIndex].getSchemaName();
                    	if (schId != null && !schId.isEmpty()) {
                    		Schema evSch = LocalObjectLoader.getSchema(schId, event[rowIndex].getSchemaVersion());
                    		return evSch.getName()+" v"+event[rowIndex].getSchemaVersion();
                    	}
                    	return null;
                    	
                    case 7: return event[rowIndex].getViewName();
					default: return "";
				}
			} catch (Exception e) {
				return null;
			}
		}
		
		public Event getEventForRow(int rowIndex) {
			return event[rowIndex];
		}

		/**
		 * @see javax.swing.table.TableModel#isCellEditable(int, int)
		 */
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

	}

	@Override
	public void control(String control, String msg) {
		if (control.equals(MemberSubscription.END))
			MainFrame.progress.stopBouncing("History loading complete.");
		else
			MainFrame.progress.stopBouncing("History: "+msg);
	}

}
