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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;


@SuppressWarnings("serial")
public class RoleAdmin extends Box implements ActionListener {

    AgentProxy agent;
    JTable table;
    RoleTableModel model = new RoleTableModel();
    JButton addButton;
    JButton removeButton;
    JButton saveButton;

    public RoleAdmin() {
        super(BoxLayout.Y_AXIS);

        table = new JTable(model);
        add(new JScrollPane(table));

        add(Box.createVerticalGlue());
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        addButton = new JButton("Add");
        buttonBox.add(addButton);
        buttonBox.add(Box.createHorizontalGlue());
        removeButton = new JButton("Remove");
        buttonBox.add(removeButton);
        buttonBox.add(Box.createHorizontalGlue());
        saveButton = new JButton("Save");
        buttonBox.add(saveButton);
        buttonBox.add(Box.createHorizontalGlue());
        add(buttonBox);
        
        addButton.setActionCommand("add");
        addButton.addActionListener(this);
        removeButton.setActionCommand("remove");
        removeButton.addActionListener(this);
        saveButton.setActionCommand("save");
        saveButton.addActionListener(this);
        addButton.setEnabled(true);
        removeButton.setEnabled(true);
        saveButton.setEnabled(false);
        

    }

    public void setEntity(AgentProxy agent) {
        this.agent = agent;
        model.loadPaths();
    }

   @Override
   public void actionPerformed(ActionEvent e) {
       if (e.getActionCommand().equals("add")) {
           String newPath = JOptionPane.showInputDialog(this, "Enter new path", "Add Role Path", JOptionPane.PLAIN_MESSAGE);
           model.addRole(newPath);
           saveButton.setEnabled(true);
       }
       else if (e.getActionCommand().equals("remove")) {
           if (table.getSelectedRow() > -1) {
               model.removeRole(table.getSelectedRow());
               saveButton.setEnabled(true);
           }
       }
       else if (e.getActionCommand().equals("save")) {
    	   try {
			MainFrame.userAgent.execute(agent, "SetAgentRoles", model.getRolePaths());
			saveButton.setEnabled(false);
    	   } catch (Exception ex) {
			MainFrame.exceptionDialog(ex);
    	   }
       }
   }


    private class RoleTableModel extends AbstractTableModel {
        ArrayList<RolePath> rolePaths;
        public RoleTableModel() {
            rolePaths = new ArrayList<RolePath>();
        }

        public void removeRole(int selectedRow) {
        	if (rolePaths.size()>0)
        		rolePaths.remove(selectedRow);
        	if (rolePaths.size() < 1)
        		removeButton.setEnabled(false);
        	fireTableDataChanged();
		}

		public String[] getRolePaths() {
			String[] roleNames = new String[rolePaths.size()];
			int i=0;
			for (RolePath role : rolePaths) {
				roleNames[i++] = role.toString();
			}
			return roleNames;
		}

		public void addRole(String newPath) {
			RolePath newRole;
			try {
				newRole = Gateway.getLookup().getRolePath(newPath);
			} catch (ObjectNotFoundException e) {
				MainFrame.exceptionDialog(e);
				return;
			}
			if (rolePaths.contains(newRole)) {
				JOptionPane.showMessageDialog(null, "Role already assigned to agent", "Error assigning Role", JOptionPane.ERROR_MESSAGE);
				return;
			}
			rolePaths.add(newRole);
			removeButton.setEnabled(true);
			fireTableDataChanged();
		}

		public void loadPaths() {
        	rolePaths.clear();
            for (RolePath currentRole: Gateway.getLookup().getRoles(agent.getPath())) {
            	rolePaths.add(currentRole);
            }
            fireTableDataChanged();
        }
        
        @Override
		public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
		public int getColumnCount() {
            return 1;
        }

        @Override
		public String getColumnName(int column) {
            return "Role";
        }

        @Override
		public int getRowCount() {
            return rolePaths.size();
        }

        @Override
		public Object getValueAt(int rowIndex, int columnIndex) {
            return rolePaths.get(rowIndex).toString();
        }

        @Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
