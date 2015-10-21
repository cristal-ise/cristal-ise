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
package org.cristalise.gui.tabs;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.gui.tabs.outcome.OutcomeHandler;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;


public class ViewpointPane extends ItemTabPane implements ItemListener, ActionListener, ProxyObserver<C2KLocalObject> {

    JComboBox<String> schemas;
    JComboBox<Viewpoint> views;
    JComboBox<EventItem> events;
    JLabel eventDetails;
    JButton exportButton;
    JButton viewButton;

    ArrayList<String> schemaList;
    ArrayList<Viewpoint> viewpointList;
    ArrayList<EventItem> eventList;
    String currentSchema = null;
    Outcome currentOutcome = null;
    OutcomeHandler thisOutcome;
    boolean suspendSelection = false;

    JPanel dataView = new JPanel(new GridLayout(1,1));

    public ViewpointPane() {

        super("Data Viewer", "Outcome Browser");
        initialize();
    }

    public void initialize() {
        initPanel();

        // Set up view box

        Box viewBox = Box.createHorizontalBox();

        JLabel label = new JLabel("Outcome Type:", SwingConstants.LEFT);
        viewBox.add(label);
        viewBox.add(Box.createHorizontalStrut(7));

        schemas = new JFixedHeightComboBox<String>();
        viewBox.add(schemas);
        viewBox.add(Box.createHorizontalGlue());
        schemas.addItemListener(this);

        label = new JLabel("View:", SwingConstants.LEFT);
        viewBox.add(label);
        viewBox.add(Box.createHorizontalStrut(7));

        views = new JFixedHeightComboBox<Viewpoint>();
        viewBox.add(views);
        viewBox.add(Box.createHorizontalGlue());
        views.addItemListener(this);
        viewBox.setMaximumSize(views.getMaximumSize());
        this.add(viewBox);

        // Set up event details box
        Box eventBox = Box.createHorizontalBox();

        label = new JLabel("Event:", SwingConstants.LEFT);
        eventBox.add(label);
        eventBox.add(Box.createHorizontalStrut(7));

        events = new JFixedHeightComboBox<EventItem>();
        eventBox.add(events);
        eventBox.add(Box.createHorizontalStrut(7));
        events.addItemListener(this);

        eventDetails = new JLabel();
        eventBox.add(eventDetails);
        eventBox.add(Box.createHorizontalGlue());

        if (MainFrame.isAdmin) {
            viewButton = new JButton("Write View");
            viewButton.setMargin(new Insets(0, 0, 0, 0));
            viewButton.setActionCommand("setview");
            eventBox.add(viewButton);
            eventBox.add(Box.createHorizontalStrut(14));
            viewButton.addActionListener(this);
        }

        exportButton = new JButton("Export");
        exportButton.setMargin(new Insets(0, 0, 0, 0));
        exportButton.setActionCommand("export");
        exportButton.addActionListener(this);
        eventBox.add(exportButton);
        eventBox.setMaximumSize(events.getMaximumSize());
        this.add(eventBox);
        add(Box.createVerticalStrut(5));

        // data pane
        this.add(dataView);
    }

    @Override
	public void reload() {
        // reset boxes
        schemas.removeAllItems();
        views.removeAllItems();
        events.removeAllItems();
        eventDetails.setText("");

        clearView();

        // reload
        initForItem(sourceItem);
    }

    @Override
	public void run() {
        Thread.currentThread().setName("Viewpoint Pane Builder");
        //Local object subscriptions
        sourceItem.getItem().subscribe(new MemberSubscription<C2KLocalObject>(this, ClusterStorage.VIEWPOINT, false));
        sourceItem.getItem().subscribe(new MemberSubscription<C2KLocalObject>(this, ClusterStorage.OUTCOME, false));
        clearView();
        schemas.addItem("--");
        currentSchema = null;
        schemaList = new ArrayList<String>();
        try {
            String outcomeTypes = sourceItem.getItem().queryData(ClusterStorage.VIEWPOINT+"/all");
            StringTokenizer tok = new StringTokenizer(outcomeTypes, ",");
            int nonSystemSchemas = 0; String defaultSelection = null;
            while (tok.hasMoreTokens()) {
                String thisType = tok.nextToken();
                schemas.addItem(thisType);
                schemaList.add(thisType);
                if (thisType.equals("PredefinedStepOutcome") || thisType.equals("ItemInitialization"))
                	continue;
                nonSystemSchemas++; defaultSelection = thisType;
            }
            if (nonSystemSchemas == 1) schemas.setSelectedItem(defaultSelection);
        } catch (Exception e) {
            Logger.msg(2, "No viewpoints found");
        }
    }

    @Override
	public void itemStateChanged(ItemEvent e) {

        Object selectedItem = e.getItem();
        if (e.getStateChange() == ItemEvent.DESELECTED) return;
        if (suspendSelection) return;
        if (e.getItem().equals("--")) return;

        if (e.getItemSelectable() == schemas)
            switchSchema((String)selectedItem);
        else if (e.getItemSelectable() == views)
            switchView((Viewpoint)selectedItem);
        else if (e.getItemSelectable() == events)
            showEvent((EventItem)selectedItem);
    }

    public void switchSchema(String schemaName) {
        clearView();
        suspendSelection = true;
        views.removeAllItems();
        events.removeAllItems();
        viewpointList = new ArrayList<Viewpoint>();
        eventList = new ArrayList<EventItem>();

        currentSchema = schemaName;

        try {
            // populate views
            String viewNames = sourceItem.getItem().queryData(ClusterStorage.VIEWPOINT+"/"+schemaName+"/all");
            StringTokenizer tok = new StringTokenizer(viewNames, ",");
            Viewpoint lastView = null;
            while(tok.hasMoreTokens()) {
                String viewName = tok.nextToken();
                Viewpoint thisView = (Viewpoint)sourceItem.getItem().getObject(ClusterStorage.VIEWPOINT+"/"+schemaName+"/"+viewName);
                views.addItem(thisView);
                if (lastView == null) lastView = thisView;
                if (thisView.getName().equals("last")) //select
                    lastView = thisView;
                viewpointList.add(thisView);
            }

            String ocVersions = sourceItem.getItem().queryData(ClusterStorage.OUTCOME+"/"+schemaName+"/all");
            tok = new StringTokenizer(ocVersions, ",");
            while(tok.hasMoreTokens()) {
                int schemaVersion = Integer.parseInt(tok.nextToken());
                String ocEvents = sourceItem.getItem().queryData(ClusterStorage.OUTCOME+"/"+schemaName+"/"+schemaVersion+"/all");
                StringTokenizer tok2 = new StringTokenizer(ocEvents, ",");
                    while(tok2.hasMoreTokens()) {
                        int eventId = Integer.parseInt(tok2.nextToken());
                        EventItem newEvent = new EventItem(eventId, schemaVersion);
                        for (Viewpoint thisView : viewpointList) {
                            if (thisView.getEventId() == eventId)
                                newEvent.addView(thisView.getName());
                        }
                        eventList.add(newEvent);
                    }
                    Collections.sort(eventList, new Comparator<EventItem>() {
                        @Override
						public int compare(EventItem o1, EventItem o2) {
                            return o1.compareTo(o2);
                        }
                    });
                    for (EventItem eventItem : eventList)
						events.addItem(eventItem);
            }

            if (lastView != null) {
                suspendSelection = false;
                views.setSelectedItem(lastView);
                switchView(lastView);
            }

        } catch (Exception e) {
            Logger.error(e);
            JOptionPane.showMessageDialog(this,
                "The data structures of this item are incorrect.\nPlease contact your administrator.",
                "Viewpoint Error" , JOptionPane.ERROR_MESSAGE);
        }
        suspendSelection = false;
    }

    public void switchView(Viewpoint newView) {
        for (EventItem thisEvent : eventList) {
            if (thisEvent.eventId == newView.getEventId()) {
                suspendSelection = true;
                events.setSelectedItem(thisEvent);
                showEvent(thisEvent);
                suspendSelection = false;
                break;
            }
        }
    }

    public void showEvent(EventItem thisEvent) {
        eventDetails.setText(thisEvent.getEventDesc());
        try {
            setView((Outcome)sourceItem.getItem().getObject(
                ClusterStorage.OUTCOME+"/"+currentSchema+"/"+thisEvent.schemaVersion+"/"+thisEvent.eventId));
        } catch (Exception ex) {
            Logger.error(ex);
            JOptionPane.showMessageDialog(this,
                "Could not retrieve requested outcome.\nPlease contact your administrator.",
                "Viewpoint Error" , JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setView(Outcome data) {
		Logger.msg(6, "ViewpointPane: got outcome type: "+data.getSchemaType()+" version: "+data.getSchemaVersion());
        String schema;
        currentOutcome = data;
        dataView.removeAll();
        String error = null;
        try {
    		schema = LocalObjectLoader.getSchema(data.getSchemaType(), data.getSchemaVersion()).schema;
    		thisOutcome = ItemTabPane.getOutcomeHandler(data.getSchemaType(), data.getSchemaVersion());
            thisOutcome.setDescription(schema);
            thisOutcome.setOutcome(data.getData());
            thisOutcome.setReadOnly(true);
		    Thread builder = new Thread(thisOutcome);
		    builder.start();
            dataView.add(thisOutcome.getPanel());
            exportButton.setEnabled(true);
            if (viewButton!=null) viewButton.setEnabled(true);
            return;
        } catch (ObjectNotFoundException ex) {
            error = "Schema not found";
        } catch (OutcomeException ex) {
            error = "Outcome was not valid. See log for details: "+ex.getMessage();
            Logger.error(ex);
        }

        dataView.add(new JLabel(error));
    }

    public void clearView() {
        dataView.removeAll();
        exportButton.setEnabled(false);
        if (viewButton!=null) viewButton.setEnabled(false);
    }

    @Override
	public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("export") && currentOutcome != null)
            saveOutcomeToFile();
        if (e.getActionCommand().equals("setview") && currentOutcome != null)
            overrideView();
    }

    private void saveOutcomeToFile() {

        MainFrame.xmlChooser.setSelectedFile(new File(currentOutcome.getSchemaType()+".xml"));
        int returnVal = MainFrame.xmlChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File targetFile = MainFrame.xmlChooser.getSelectedFile();
            if (!(targetFile.getAbsolutePath().endsWith(".xml")))
                targetFile = new File(targetFile.getAbsolutePath()+".xml");

            Logger.msg(2, "ViewpointPane.actionPerformed() - Exporting outcome to file " + targetFile.getName());
            try {
                thisOutcome.export(targetFile);
            }
            catch (Exception ex) {
                Logger.error(ex);
                MainFrame.exceptionDialog(ex);
            }
        }

    }

    private void overrideView() {

        Viewpoint oldView = (Viewpoint)views.getSelectedItem();
        EventItem newEvent = (EventItem)events.getSelectedItem();

        if (oldView.getEventId() == newEvent.eventId) {
            JOptionPane.showMessageDialog(this,
            "View '"+oldView.getName()+"' is already set to event "+newEvent.eventId,
            "Viewpoint Already Set" , JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (JOptionPane.showConfirmDialog(this,
                "Are you sure you want to set the '"+oldView.getName()+
                "' view to event " + newEvent.eventId+ "?",
                "Overwrite view",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;

        try {
            String[] predefParams = new String[3];
            predefParams[0] = oldView.getSchemaName();
            predefParams[1] = oldView.getName();
            predefParams[2] = String.valueOf(newEvent.eventId);
            MainFrame.userAgent.execute(sourceItem.getItem(), "WriteViewpoint", predefParams);
        } catch (Exception e) {
            Logger.error(e);
            MainFrame.exceptionDialog(e);
        }
    }

    @Override
	public void runCommand(String command) {
		String[] viewElements = command.split(":");
		if (viewElements.length!=2) return;
		if (schemaList == null) run();
		schemas.setSelectedItem(viewElements[0]);
		for (Viewpoint thisView : viewpointList) {
			if (thisView.getName().equals(viewElements[1])) {
				switchView(thisView);
				return;
			}
		}
		Logger.error("Viewpoint "+command+" not found in this item");
	}

	@Override
	public void add(C2KLocalObject contents) {
        if (contents instanceof Viewpoint)
            addViewpoint((Viewpoint)contents);
        else if (contents instanceof Outcome)
            addOutcome((Outcome)contents);

    }

    public void addViewpoint(Viewpoint newView) {
        String schemaName = newView.getSchemaName();
        Logger.msg(3, "Viewpoint "+newView.getName()+" now points to "+newView.getEventId());
        if (!(schemaList.contains(schemaName))) {
            schemaList.add(schemaName);
            schemas.addItem(schemaName);
            return;
        }


        if (!(schemaName.equals(schemas.getSelectedItem())))
            return;

        for (EventItem thisEvent : eventList) {
            if (thisEvent.eventId == newView.getEventId())
                thisEvent.addView(newView.getName());
            else
                thisEvent.removeView(newView.getName());
        }

        boolean isSelected = false;
        for (Viewpoint thisView : viewpointList) {
            if (thisView.getName().equals(newView.getName())) {
                isSelected = thisView.equals(views.getSelectedItem());
                views.removeItem(thisView);
                viewpointList.remove(thisView);
                break;
            }
        }

        views.addItem(newView);
        viewpointList.add(newView);
        if (isSelected) {
            views.setSelectedItem(newView);
        }

    }

    public void addOutcome(Outcome contents) {
        if (!(contents.getSchemaType().equals(currentSchema))) // not interested
            return;
        Logger.msg(3, "Adding event "+contents.getID());
        EventItem newEvent = new EventItem(contents.getID(), contents.getSchemaVersion());
        eventList.add(newEvent);
        events.addItem(newEvent);
    }

    @Override
	public void remove(String id) {
        // we don't really remove viewpoints
    }

    class EventItem implements Comparable<EventItem> {
        public int eventId;
        public int schemaVersion;
        public ArrayList<String> viewNames = new ArrayList<String>();
        public String viewList = "";

        public EventItem(int eventId, int schemaVersion) {
            this.eventId = eventId;
            this.schemaVersion = schemaVersion;
        }

        public void addView(String viewName) {
            if (!(viewNames.contains(viewName))) {
                viewNames.add(viewName);
                buildViewLabel();
            }
        }

        public void removeView(String viewName) {
            viewNames.remove(viewName);
            buildViewLabel();
        }

        private void buildViewLabel() {
            if (viewNames.size() == 0) {
                viewList = "";
                return;
            }

            StringBuffer newLabel = new StringBuffer(" (");
            for (Iterator<String> iter = viewNames.iterator(); iter.hasNext();) {
                String viewName = iter.next();
                newLabel.append(viewName);
                if (iter.hasNext())
                    newLabel.append(", ");
            }

            viewList = newLabel.append(")").toString();
        }

        @Override
		public String toString() {
            return eventId+viewList;

        }

        public String getEventDesc() {
            try {
                Event myEvent = (Event)sourceItem.getItem().getObject(ClusterStorage.HISTORY+"/"+eventId);
                return ("Recorded on "+myEvent.getTimeString()+
                    " by "+myEvent.getAgentPath().getAgentName()+
                    " using schema v"+schemaVersion);
            } catch (Exception ex) {
                Logger.error(ex);
                return ("Error retrieving event details");
            }
        }

        @Override
		public int compareTo(EventItem other) {
            if (other.eventId < eventId) return 1;
            if (other.eventId > eventId) return -1;
            return 0;
        }
    }

	@Override
	public void control(String control, String msg) {
		// TODO Auto-generated method stub
		
	}
}
