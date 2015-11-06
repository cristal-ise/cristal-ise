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
package org.cristalise.gui.tabs.execution;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.tabs.ExecutionPane;
import org.cristalise.gui.tabs.ItemTabPane;
import org.cristalise.gui.tabs.outcome.InvalidOutcomeException;
import org.cristalise.gui.tabs.outcome.InvalidSchemaException;
import org.cristalise.gui.tabs.outcome.OutcomeHandler;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;


public class ActivityViewer extends JPanel implements Runnable {

    ItemProxy item;
    Box outcomeButtons = Box.createHorizontalBox();
    OutcomeHandler outcomePanel;
    OutcomeHandler errorPanel;
    JPanel outcomeView = new JPanel(new GridLayout(1,1));
    JPanel errorView = new JPanel(new GridLayout(2,1));
    ActivityItem thisAct;
    ArrayList<RequestButton> requestButtons = new ArrayList<RequestButton>();
    JLabel noOutcome = new JLabel("No outcome data is required for this activity");
    ExecutionPane parent;
    JLabel status;
    JComboBox executors;
    JButton saveButton  = new JButton("Save");
    JButton loadButton  = new JButton("Load");
    GridBagLayout gridbag = new GridBagLayout();
    Job executingJob = null;
    static JFileChooser chooser = new JFileChooser();
    static {
        chooser.addChoosableFileFilter(
            new javax.swing.filechooser.FileFilter() {
                @Override
				public String getDescription() {
                    return "XML Files";
                }
                @Override
				public boolean accept(File f) {
                    if (f.isDirectory() || (f.isFile() && f.getName().endsWith(".xml"))) {
                        return true;
                    }
                    return false;
                }
            });
    }

    public ActivityViewer (ActivityItem newAct, ItemProxy item, ExecutionPane parent){
        thisAct = newAct;
        this.item = item;
        this.parent = parent;
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx=0; c.gridy=1; c.weightx=1.0; c.weighty=0.0;
        c.insets = new Insets(5,5,5,5);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        Job firstJob = (thisAct.getJobs().get(0));
// desc
        String desc = firstJob.getDescription();
        if (desc != null && desc.length() > 0) {
            Box descBox = Box.createHorizontalBox();

            String chopDesc = null;
            if(desc.length() >= 80) chopDesc = desc.substring(0,80);
            else chopDesc = desc;

            descBox.add(new JLabel("Description: "+chopDesc));
            if (desc.length()>chopDesc.length()) {
                descBox.add(new JLabel(" ..."));
                descBox.add(Box.createHorizontalStrut(7));
                JButton descButton = new JButton("View");
                descButton.setMargin(new Insets(0,0,0,0));
                descButton.setActionCommand(desc);
                descButton.addActionListener(new ActionListener() {
                    @Override
					public void actionPerformed(ActionEvent e) {
                        JTextArea descArea = new JTextArea(e.getActionCommand());
                        descArea.setLineWrap(true);
                        descArea.setWrapStyleWord(true);
                        JScrollPane descScroll = new JScrollPane(descArea);
                        descScroll.setPreferredSize(new Dimension(400,150));
                        JOptionPane.showMessageDialog(null, descScroll, "Activity Description", JOptionPane.PLAIN_MESSAGE);
                    }
                });
                descBox.add(descButton);
            }

            gridbag.setConstraints(descBox, c);
            add(descBox);
        }


// agentid
        String roleName = firstJob.getAgentRole();
        if (roleName!= null && roleName.length()>0) {
            c.gridy++;
            JLabel role = new JLabel("Agent Role: "+roleName);
            gridbag.setConstraints(role, c);
            add(role);
        }

        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(outcomeButtons, c);
        add(outcomeButtons);

        executors = MainFrame.getExecutionPlugins();
        if (executors.getItemCount() > 1) {
            c.gridx++;
            gridbag.setConstraints(executors, c);
            add(executors);
            c.gridx--;
        }

        c.gridy++;

        status = new JLabel("Waiting for request");
        status.setFont(ItemTabPane.titleFont);
        gridbag.setConstraints(status, c);
        add(status);

        c.gridx++;
        Box fileBox = Box.createHorizontalBox();
        fileBox.add(saveButton); fileBox.add(Box.createHorizontalGlue()); fileBox.add(loadButton);
        gridbag.setConstraints(fileBox, c);
        add(fileBox);
        saveButton.setEnabled(false);
        loadButton.setEnabled(false);
        c.gridx--;
        c.gridwidth = 2;
        for (Object name2 : thisAct.getJobs()) {
            Job thisJob = (Job)name2;
            RequestButton newButton = new RequestButton(thisJob, this);
            requestButtons.add(newButton);
            outcomeButtons.add(newButton);
            outcomeButtons.add(Box.createHorizontalStrut(5));
            newButton.setEnabled(false);
            
            if (thisJob.hasOutcome()) {

        		Schema schema;
				try {
					schema = thisJob.getSchema();
				} catch (Exception e) {
					newButton.setToolTipText("Could not load schema for this job.");
					continue;
				} 
    
            	if(!schema.getName().equals("Errors") && outcomePanel == null) {
            		try {
            			outcomePanel = getOutcomeHandler(thisJob);
						outcomeView = outcomePanel.getPanel();
						newButton.setEnabled(true);
					} catch (ObjectNotFoundException ex) {
						outcomeView.add(new JLabel("Schema not found: "+schema.getName()+" v"+schema.getVersion()));
	              	} catch (Exception ex) {
	                    outcomeView.add(new JLabel("ERROR loading outcome editor: "
	                        +ex.getClass().getName()+" ("+ex.getMessage()+")"));
	                    Logger.error(ex);
	            	}
            	}
            	if (schema.getName().equals("Errors")) {
            		try {
            			errorPanel = getOutcomeHandler(thisJob);
            			errorView.add(errorPanel.getPanel());
            			newButton.setEnabled(true);
					} catch (Exception ex) {
						errorView.add(new JLabel("ERROR loading error editor: "
		                        +ex.getClass().getName()+" ("+ex.getMessage()+")"));
	
					}
            	}
            }
            else
            	newButton.setEnabled(true);
        }
        if (outcomePanel == null)
            outcomeView.add(noOutcome);
        else
        	enableLoadSaveButtons();


        c.gridy++; c.weighty=1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        gridbag.setConstraints(outcomeView, c);
        add(outcomeView);

    }

    public OutcomeHandler getOutcomeHandler(Job thisJob) throws ObjectNotFoundException, InvalidSchemaException, InvalidOutcomeException, InvalidDataException {
        Schema schema = thisJob.getSchema();
        OutcomeHandler thisForm;
        thisForm = ItemTabPane.getOutcomeHandler(schema.getName(), schema.getVersion());
        thisForm.setReadOnly(false);
        thisForm.setDescription(schema.getSchemaData());
        String outcomeString = thisJob.getOutcomeString();
        if ( outcomeString!= null && outcomeString.length() > 0)
        	thisForm.setOutcome(outcomeString);
        return thisForm;
    }
    
    public void enableLoadSaveButtons() {
    	saveButton.addActionListener(new ActionListener() {
    		@Override
			public void actionPerformed(ActionEvent e) {
    			String output;
    			try {
                    output = outcomePanel.getOutcome();
                    int returnVal = chooser.showSaveDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File targetFile = chooser.getSelectedFile();
                        if (!(targetFile.getAbsolutePath().endsWith(".xml")))
                            targetFile = new File(targetFile.getAbsolutePath()+".xml");

                        Logger.msg(2, "ExecutionPane - Exporting outcome to file " + targetFile.getName());
                        FileStringUtility.string2File(targetFile, output);
                    }
    			} catch (Exception ex) {
                     Logger.error(ex);
                     MainFrame.exceptionDialog(ex);
                    }
    		}
    	});
    	saveButton.setEnabled(true);

    	loadButton.addActionListener(new ActionListener() {
    		@Override
			public void actionPerformed(ActionEvent e) {
    			try {
                    int returnVal = chooser.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File targetFile = chooser.getSelectedFile();

                        Logger.msg(2, "ViewpointPane.actionPerformed() - Reading outcome from file " + targetFile.getName());
                        String outcome = FileStringUtility.file2String(targetFile);
                        outcomePanel.setOutcome(outcome);
                        new Thread(outcomePanel).start();
                    }
    			} catch (Exception ex) {
                     Logger.error(ex);
                     MainFrame.exceptionDialog(ex);
                    }
    		}
    	});
    	loadButton.setEnabled(true);
    }
    
    public void init() {
        if (outcomePanel != null)
            new Thread(outcomePanel).start();
        if (errorPanel != null)
        	new Thread(errorPanel).start();
    }

    public void execute(Job thisJob) {
        try {
            if (thisJob.hasOutcome())
            	if (!thisJob.getSchema().getName().equals("Errors"))
            		thisJob.setOutcome(outcomePanel.getOutcome());
            	else {
	            	Box errorBox = Box.createVerticalBox();
	            	errorBox.add(new JLabel("Please give details of the error:"));
	            	errorBox.add(errorView);
	            	int result = JOptionPane.showConfirmDialog(this, errorBox, "Send Error", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
	            	if (result != JOptionPane.OK_OPTION)
	            		return;
	            	thisJob.setOutcome(errorPanel.getOutcome());
            	}
            executingJob = thisJob;
            new Thread(this).start();
        } catch (Exception ex) {
        	MainFrame.exceptionDialog(ex);
        }

    }

    /**
     *  Submits the job to the database
     */
    @Override
	public void run() {
        Thread.currentThread().setName("Activity Execution");
        enableAllButtons(false);
        try {
            Executor selectedExecutor = (Executor)executors.getSelectedItem();
            selectedExecutor.execute(executingJob, status);
        } catch (Exception e) {
            Logger.error(e);
            MainFrame.progress.stopBouncing("Error during execution");
            status.setText("Error during execution: "+e.getClass().getSimpleName());
            MainFrame.exceptionDialog(e);
        }
        enableAllButtons(true);
    }

    private void enableAllButtons(boolean enabled) {

        for (RequestButton thisButton : requestButtons) {
            thisButton.setEnabled(enabled);
        }
    }

    public ActivityItem getActivity() {
        return thisAct;
    }
}
