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
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.cristalise.gui.ItemDetails;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.persistency.C2KLocalObjectMap;
import org.cristalise.kernel.process.Gateway;
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
        Gateway.getStorage().clearCache(parent.getItemPath(), JOB);
        joblist.keySet();
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

        reload();

        model = new JoblistTableModel();
        eventTable.setModel(model);
        model.setView();
    }

    public void add(Job contents) {
        reload();
    }

    private class JoblistTableModel extends AbstractTableModel {
        Job[]     jobArray;

        public JoblistTableModel() {
        }

        public void setView() {
            jobArray = new Job[joblist.size()];
            int i = 0;
            for (String key : joblist.keySet()) jobArray[i++] = joblist.get(key);
            fireTableStructureChanged();
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
                case 0: return "Activity";
                case 1: return "Transition";
                case 2: return "Schema";
                case 3: return "Script";
                case 4: return "StateMachine";
                default: return "";
            }
        }

        /**
         * @see javax.swing.table.TableModel#getRowCount()
         */
        @Override
        public int getRowCount() {
            return jobArray != null ? jobArray.length : 0;
        }

        /**
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (jobArray.length <= rowIndex || jobArray[rowIndex] == null) {
                log.warn("JoblistTableModel.getValueAt() - INVALID rowIndex:{}", rowIndex);
                return "";
            }

            try {
                switch (columnIndex) {
                    case 0: return jobArray[rowIndex].getStepName();
                    case 1: return jobArray[rowIndex].getTransitionName();
                    case 2: return jobArray[rowIndex].getSchema() != null ? jobArray[rowIndex].getSchemaName() : "";
                    case 3: return jobArray[rowIndex].getScript() != null ? jobArray[rowIndex].getScriptName() : "";
                    case 4: return jobArray[rowIndex].getStateMachine() != null ? jobArray[rowIndex].getStateMachine().name : "";
                    default: return "";
                }
            }
            catch (Exception e) {
                log.warn("JoblistTableModel.getValueAt() - rowIndex:{} columnIndex:{}", rowIndex, columnIndex, e);
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
    }
}
