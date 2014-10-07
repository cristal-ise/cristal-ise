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

import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.tabs.execution.ActivityItem;
import org.cristalise.gui.tabs.execution.ActivityViewer;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.utils.Language;
import org.cristalise.kernel.utils.Logger;


public class ExecutionPane extends ItemTabPane implements ProxyObserver<Workflow> {

    ArrayList<Job> jobList = null;
    Object jobLock = new Object();
    ActivityItem emptyAct = new ActivityItem();
    JLabel noActs = new JLabel(Language.translate("There are currently no activities that you can execute in this item."));
    JPanel view = new JPanel(new GridLayout(1, 1));
    ActivityViewer currentActView;
    JComboBox activitySelector = new JComboBox();
    Box activityBox = Box.createHorizontalBox();
    String selAct = null;
    ArrayList<ActivityItem> activities;
    String autoRun = null;
    boolean init = false;
    boolean formIsActive = false;
    public ExecutionPane() {
        super("Execution", "Activity Execution");
        super.initPanel();
        // add view panel
        c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 1; c.weightx = 1.0; c.weighty = 2.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        gridbag.setConstraints(view, c);

        add(view);
        // create activity selection box
        activityBox.add(new JLabel(Language.translate("Select Activity") + ": "));
        activityBox.add(Box.createHorizontalStrut(5));
        activitySelector.setEditable(false);
        activityBox.add(activitySelector);
        activitySelector.addItemListener(new ItemListener() {
            @Override
			public void itemStateChanged(ItemEvent selection) {
                if (selection.getStateChange() == ItemEvent.SELECTED) {
                    selectActivity(selection.getItem());
                }
            }
        });
    }
    @Override
	public void run() {
        Thread.currentThread().setName("Execution Pane Builder");
        sourceItem.getItem().subscribe(new MemberSubscription<Workflow>(this, ClusterStorage.LIFECYCLE, false));
        loadJobList();
        init = true;
        if (autoRun != null) {
            runCommand(autoRun);
            autoRun = null;
        }
    }
    private void loadJobList() {
        synchronized (jobLock) {
            activitySelector.removeAllItems();
            view.removeAll();
            activities = new ArrayList<ActivityItem>();
            try {
                jobList = (sourceItem.getItem()).getJobList(MainFrame.userAgent);
                activitySelector.addItem(emptyAct);
                for (Job thisJob : jobList) {
                    //Logger.msg(7, "ExecutionPane - loadJobList " + thisJob.hasOutcome() + "|" + thisJob.getSchemaName() + "|" + thisJob.getSchemaVersion() + "|");
                    ActivityItem newAct = new ActivityItem(thisJob);
                    if (activities.contains(newAct)) {
                        int actIndex = activities.indexOf(newAct);
                        activities.get(actIndex).addJob(thisJob);
                    } else {
                        Logger.msg(2, "ExecutionPane - Adding activity " + thisJob.getStepPath());
                        addActivity(newAct);
                    }
                }
            } catch (Exception e) {
                Logger.error("Error fetching joblist");
                Logger.error(e);
            }

            switch (activities.size()) {
                case 0 :
                    view.add(noActs);
                    break;
                case 1 :
                    currentActView = new ActivityViewer(activities.get(0), sourceItem.getItem(), this);
                    c.fill = GridBagConstraints.BOTH;
                    gridbag.setConstraints(view, c);
                    view.add(currentActView);
                    currentActView.init();
                    break;
                default :
                    c.fill = GridBagConstraints.HORIZONTAL;
                    gridbag.setConstraints(view, c);
                    view.add(activityBox);
            }
        }
        revalidate();
        updateUI();
    }
    @Override
	public void reload() {
        loadJobList();
    }
    private void addActivity(ActivityItem newAct) {
        if (activities.contains(newAct)) {
            Logger.msg(6, "ExecutionPane.addActivity(): Already in " + newAct.getStepPath());
            int actIndex = activities.indexOf(newAct);
            activitySelector.removeItemAt(actIndex);
            activitySelector.insertItemAt(newAct, actIndex);
            activities.set(actIndex, newAct);
        } else {
            Logger.msg(6, "ExecutionPane.addActivity(): New " + newAct.getStepPath());
            activities.add(newAct);
            activitySelector.addItem(newAct);
        }
    }
    private void selectActivity(Object selObj) {
        if (selObj.equals(emptyAct))
            return;
        view.removeAll();
        c.fill = GridBagConstraints.BOTH;
        gridbag.setConstraints(view, c);
        currentActView = new ActivityViewer((ActivityItem)selObj, sourceItem.getItem(), this);
        view.add(currentActView);
        revalidate();
        updateUI();
        currentActView.init();
    }
    @Override
	public void runCommand(String command) {
        if (init) {
            for (ActivityItem act : activities) {
                if (act.name.equals(command)) {
                    selectActivity(act);
                }
            }
        } else
            autoRun = command;
    }
    /**
     * when the workflow changes, reload this pane.
     */
    @Override
	public void add(Workflow contents) {
        if (!formIsActive)
            reload();
        else { // look to see if this form is now invalid
            // get the new joblist
            try {
                jobList = (sourceItem.getItem()).getJobList(MainFrame.userAgent);
            } catch (Exception ex) {
                return;
            }
            // compare to currently editing jobs
            ArrayList<?> currentActJobs = currentActView.getActivity().getJobs();
            boolean allValid = true;
            for (Iterator<?> iter = currentActJobs.iterator(); iter.hasNext() && allValid;) {
                Job thisJob = (Job)iter.next();
                boolean stillValid = false;
                for (Job newJob : jobList) {
                    if (thisJob.equals(newJob)) {
                        stillValid = true;
                        break;
                    }
                }
                allValid &= stillValid;
            }
            if (!allValid) { // not all transitions are now valid
                reload(); // refresh the execution pane
            }
        }
    }
    /**
     * Not pertinent for this one
     */
    @Override
	public void remove(String id) {
    }
	@Override
	public void control(String control, String msg) {
		// TODO Auto-generated method stub
		
	}
}
