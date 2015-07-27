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
package org.cristalise.gui.lifecycle.instance;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.graph.view.SelectedVertexPanel;
import org.cristalise.gui.tabs.ItemTabPane;
import org.cristalise.gui.tabs.execution.Executor;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.stateMachine.State;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * $Revision: 1.8 $
 * $Date: 2005/09/07 13:46:31 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class TransitionPanel extends SelectedVertexPanel implements ActionListener {
    protected Activity mCurrentAct;
    protected GridBagLayout gridbag;
    protected GridBagConstraints c;
    protected Box transBox;
    protected JComboBox executors;
    protected JComboBox states = new JComboBox();
    protected JCheckBox active = new JCheckBox();
    protected JLabel status = new JLabel();
    protected ItemProxy mItem;

    public TransitionPanel() {
        super();
        gridbag = new GridBagLayout();
        setLayout(gridbag);
        c = new GridBagConstraints();
        c.gridx=0; c.gridy=0;
        c.weightx=1; c.weighty=0;
        c.fill=GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Available Transitions");
        title.setFont(ItemTabPane.titleFont);
        gridbag.setConstraints(title, c);
        add(title);

        c.gridy++;
        gridbag.setConstraints(status, c);
        add(status);
        c.gridy++;

        transBox = Box.createHorizontalBox();
        gridbag.setConstraints(transBox, c);
        add(transBox);

        c.weightx=0; c.gridx++;
        executors = MainFrame.getExecutionPlugins();
        if (executors.getItemCount() > 1) {
            gridbag.setConstraints(executors, c);
            add(executors);
        }



        if (MainFrame.isAdmin) {
            c.gridx=0; c.gridy++;
            title = new JLabel("State Hacking");
            title.setFont(ItemTabPane.titleFont);
            gridbag.setConstraints(title, c);
            add(title);
        	Box hackBox = Box.createHorizontalBox();
        	hackBox.add(states);
        	hackBox.add(Box.createHorizontalGlue());
        	hackBox.add(new JLabel("Active:"));
        	hackBox.add(active);
        	c.gridy++;
            gridbag.setConstraints(hackBox, c);
            add(hackBox);
            states.addActionListener(this);
            active.addActionListener(this);
        }

        clear();

    }
    /**
     *
     */
    @Override
	public void select(Vertex vert) {
	    clear();
	    if (!(vert instanceof Activity)) return;
        mCurrentAct = (Activity)vert;
        StateMachine sm;
		try {
			sm = mCurrentAct.getStateMachine();
		} catch (InvalidDataException e) {
			status.setText("Invalid state machine.");
			Logger.error(e);
			return;
		}
        states.removeAllItems();
        int currentState;
		try {
			currentState = mCurrentAct.getState();
		} catch (InvalidDataException e) {
			status.setText("Could not find activity state");
			Logger.error(e);
			return;
		}
        for (State thisState : sm.getStates()) {
			states.addItem(thisState);
			if (currentState == thisState.getId())
				states.setSelectedItem(thisState);
		}
        states.setEnabled(true);
        active.setSelected(mCurrentAct.active);
        active.setEnabled(true);
        Logger.msg(1, "Retrieving possible transitions for activity "+mCurrentAct.getName());
        Map<Transition, String> transitions;
		try {
			transitions = mCurrentAct.getStateMachine().getPossibleTransitions(mCurrentAct, MainFrame.userAgent.getPath());
		} catch (Exception e) {
			status.setText("Error loading possible transitions of activity. See log.");
			Logger.error(e);
			return;
		}
		
        if (transitions.size() == 0) {
            status.setText("None");
            return;
        }
        
        for (Transition trans:transitions.keySet()) {
        	boolean hasOutcome = trans.hasOutcome(mCurrentAct.getProperties());
            if (!hasOutcome || (hasOutcome && !trans.getOutcome().isRequired())) {
                JButton thisTrans = new JButton(trans.getName());
                thisTrans.setActionCommand("Trans:"+trans.getId());
                thisTrans.addActionListener(this);
                transBox.add(thisTrans);
                transBox.add(Box.createHorizontalGlue());
            }
            status.setText(transitions.size()+" transitions possible.");
        }
        revalidate();
    }

    @Override
	public void actionPerformed(ActionEvent e) {
    	if (active.isEnabled()) {
    		if (e.getSource() == active && mCurrentAct != null) {
    			mCurrentAct.active = active.isSelected();
    			return;
    		}
    	}
        if (states.isEnabled()) {    		
    		if (e.getSource() == states && mCurrentAct != null) {
    			Logger.msg(1, "Setting state of "+mCurrentAct.getName()+" to "+states.getSelectedItem());
    			mCurrentAct.setState(states.getSelectedIndex());
    			return;
    		}
    	}
    	if (!e.getActionCommand().startsWith("Trans:")) return;
        int transition = Integer.parseInt(e.getActionCommand().substring(6));
        Logger.msg("Requesting transition "+transition);
        try {
        	StateMachine actSM = mCurrentAct.getStateMachine();
        	Job thisJob = new Job(mCurrentAct,      		
        					mItem.getPath(),
                            actSM.getTransition(transition),
                            MainFrame.userAgent.getPath(),
                            "Admin");
            Executor selectedExecutor = (Executor)executors.getSelectedItem();
            selectedExecutor.execute(thisJob, status);
        } catch (Exception ex) {
            String className = ex.getClass().getName();
            className = className.substring(className.lastIndexOf('.')+1);
            Logger.error(ex);
            JOptionPane.showMessageDialog(null, ex.getMessage(), className, JOptionPane.ERROR_MESSAGE);
        }

    }

    @Override
	public void clear() {
    	states.setEnabled(false);
        active.setEnabled(false);
    	mCurrentAct = null;
        transBox.removeAll();
        status.setText("No activity selected");
        active.setSelected(false);
        revalidate();
    }


    /**
     * @param item The mItem to set.
     */
    public void setItem(ItemProxy item) {
        mItem = item;
    }
}
