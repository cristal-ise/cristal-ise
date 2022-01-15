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

import static org.cristalise.kernel.persistency.ClusterType.JOB;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.cristalise.gui.ItemDetails;
import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.persistency.C2KLocalObjectMap;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.DateUtility;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

/**
 * Pane to display all work orders that this agent can execute, and activate them on request from the user.
 * Subscribes to NodeItem for WorkOrder objects.
 */
@SuppressWarnings("serial")
@Slf4j
public class JobListPane extends ItemTabPane {

    C2KLocalObjectMap<Job>  joblist;
    JoblistTableModel       model;
    JTable                  eventTable;
    public static final int SIZE        = 30;
    int                     currentSize = SIZE;

    public JobListPane() {
        super("Job List", "Job List");
        initPanel();

        add(Box.createVerticalStrut(5));

        // Create table
        eventTable = new JTable();
        JScrollPane eventScroll = new JScrollPane(eventTable);
        add(eventScroll);

        // detect double clicked jobs
        eventTable.addMouseListener(new JobListMouseListener());
    }
    
    @Override
    public void setParent(ItemDetails parent) {
        super.setParent(parent);

        Vertx vertx = Gateway.getVertx();
        vertx.eventBus().localConsumer(parent.getItemPath().getUUID() + "/" + JOB, message -> {
            String[] tokens = ((String) message.body()).split(":");
            String jobId = tokens[0];

            if (tokens[1].equals("DELETE")) return;

            vertx.executeBlocking(promise -> {
                try {
                    add(((AgentProxy)sourceItem.getItem()).getJob(jobId));
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

    @Override
    public void reload() {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        Thread.currentThread().setName("Joblist Pane Builder");

        try {
            joblist = (C2KLocalObjectMap<Job>) sourceItem.getItem().getObject(JOB);
        }
        catch (ObjectNotFoundException e) {
            log.error("", e);
        }

        model = new JoblistTableModel();
        eventTable.setModel(model);
    }

    public void add(Job contents) {
        reload();
    }

    private class JoblistTableModel extends AbstractTableModel {
        Job[]     job;
        String[]  itemNames;

        public JoblistTableModel() {
        }

        /**
         * @see javax.swing.table.TableModel#getColumnClass(int)
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                default:
                    return String.class;
            }
        }

        /**
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        @Override
        public int getColumnCount() {
            return 5;
        }

        /**
         * @see javax.swing.table.TableModel#getColumnName(int)
         */
        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0: return "ID";
                case 1: return "Subject";
                case 2: return "Activity";
                case 3: return "Transition";
                case 4: return "Date";
                default: return "";
            }
        }

        /**
         * @see javax.swing.table.TableModel#getRowCount()
         */
        @Override
        public int getRowCount() {
            if (job != null) return job.length;
            else return 0;
        }

        /**
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (job.length <= rowIndex || job[rowIndex] == null)
                return "";
            try {
                switch (columnIndex) {
                    case 0: return job[rowIndex].getClusterPath();
                    case 1: return itemNames[rowIndex];
                    case 2: return job[rowIndex].getStepName();
                    case 3: return job[rowIndex].getTransition().getName();
                    case 4: return DateUtility.timeToString(job[rowIndex].getCreationDate());
                    default: return "";
                }
            }
            catch (Exception e) {
                return null;
            }
        }

        /**
         * @see javax.swing.table.TableModel#isCellEditable(int, int)
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Job getJobAtRow(int rowIndex) {
            return job[rowIndex];
        }

    }

    private class JobListMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            super.mouseClicked(e);
            if (e.getClickCount() == 2) {
                Job selectedJob = model.getJobAtRow(eventTable.getSelectedRow());
                try {
                    MainFrame.itemFinder.pushNewKey(selectedJob.getItemProxy().getName());
                }
                catch (Exception ex) {
                    log.error("", ex);
                    JOptionPane.showMessageDialog(null, "No Item Found", "Job references an unknown item", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
