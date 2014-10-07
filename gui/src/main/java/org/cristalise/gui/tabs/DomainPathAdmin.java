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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.process.Gateway;


/**************************************************************************
 *
 * $Revision: 1.3 $
 * $Date: 2004/10/21 08:02:21 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class DomainPathAdmin extends Box implements ActionListener {

    ItemProxy entity;
    JTable table;
    DomainPathTableModel model;
    JButton addButton;
    JButton removeButton;

    public DomainPathAdmin() {
        super(BoxLayout.Y_AXIS);

        model = new DomainPathTableModel(this);
        table = new JTable(model);
        add(new JScrollPane(table));

        add(Box.createVerticalGlue());
        Box buttonBox = Box.createHorizontalBox();
        addButton = new JButton("Add");
        buttonBox.add(addButton);
        buttonBox.add(Box.createHorizontalGlue());
        removeButton = new JButton("Remove");
        buttonBox.add(removeButton);
        buttonBox.add(Box.createHorizontalGlue());
        add(buttonBox);

        addButton.setActionCommand("add");
        addButton.addActionListener(this);
        removeButton.setActionCommand("remove");
        removeButton.addActionListener(this);
    }

    public void setEntity(ItemProxy entity) {
        this.entity = entity;
        model.loadPaths();
    }

   @Override
public void actionPerformed(ActionEvent e) {
       if (e.getActionCommand().equals("add")) {
           String newPath = JOptionPane.showInputDialog(this, "Enter new path,", "Add Domain Path", JOptionPane.PLAIN_MESSAGE);
           addDomainPath(new DomainPath(newPath));
           model.loadPaths();
       }
       else if (e.getActionCommand().equals("remove")) {
           if (table.getSelectedRow() > -1) {
               DomainPath oldPath = model.getPath(table.getSelectedRow());
               removeDomainPath(oldPath);
               model.loadPaths();
           }
       }
   }

    public boolean removeDomainPath(DomainPath oldPath) {
        return alterDomainPath(oldPath, "Remove");
    }

    public boolean addDomainPath(DomainPath newPath) {
        return alterDomainPath(newPath, "Add");
    }

    public boolean alterDomainPath(DomainPath path, String action) {

        if (JOptionPane.showConfirmDialog(this,
                action+" "+path+"?",
                action+" Domain Path",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return false;

        String[] params = new String[1];
        params[0] = path.toString();
        try {
            MainFrame.userAgent.execute(entity, action+"DomainPath", params);
        } catch (Exception e) {
        	MainFrame.exceptionDialog(e);
            return false;
        }
        return true;
    }

    private class DomainPathTableModel extends AbstractTableModel {
        ArrayList<DomainPath> domPaths;
        DomainPathAdmin parent;
        public DomainPathTableModel(DomainPathAdmin parent) {
            this.parent = parent;
            domPaths = new ArrayList<DomainPath>();
        }

        public void loadPaths() {
            domPaths.clear();
            for (Iterator<?> currentPaths = Gateway.getLookup().search(new DomainPath(), entity.getName()); currentPaths.hasNext();) {
                DomainPath thisPath = (DomainPath)currentPaths.next();
                try {
					if (thisPath.getItemPath().equals(entity.getPath())) domPaths.add(thisPath);
				} catch (ObjectNotFoundException e) { }
            }
            fireTableDataChanged();
        }

        public DomainPath getPath(int rowIndex) {
            return domPaths.get(rowIndex);
        }
        @Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            DomainPath oldPath = domPaths.get(rowIndex);
            DomainPath newPath = new DomainPath((String)aValue);
            boolean success = parent.addDomainPath(newPath);
            if (success)
                success = parent.removeDomainPath(oldPath);
            if (success) {
                oldPath.setPath(newPath);
                fireTableDataChanged();
            }
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
            return "Path";
        }

        @Override
		public int getRowCount() {
            return domPaths.size();
        }

        @Override
		public Object getValueAt(int rowIndex, int columnIndex) {
            return domPaths.get(rowIndex).toString();
        }

        @Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
    }
}
