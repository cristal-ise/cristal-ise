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
package org.cristalise.gui.graph.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JOptionPane;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import org.cristalise.kernel.utils.CastorHashMap;


/**************************************************************************
 *
 * $Revision: 1.4 $
 * $Date: 2005/08/02 07:50:10 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class PropertyTableModel extends AbstractTableModel {

    private String[] mColumnNames = { "Name", "Value" };
    private String[] mColumnNameswAbstract = { "Name", "Value", "Abstract" };
    public CastorHashMap sourceMap = new CastorHashMap();
    public ArrayList<String> sortedNameList = new ArrayList<String>();
    boolean isEditable = false;

    public PropertyTableModel(boolean useAbstract) {
        super();
        if (useAbstract) mColumnNames = mColumnNameswAbstract;
    }

    @Override
	public int getColumnCount()
    {
        return mColumnNames.length;
    }
    @Override
	public String getColumnName(int col)
    {
        return mColumnNames[col];
    }
    @Override
	public int getRowCount()
    {
        synchronized (sourceMap) {
            return sourceMap.size();
        }
    }
    @Override
	public Object getValueAt(int rowIndex, int colIndex)
    {
        synchronized (sourceMap) {
            String rowName = sortedNameList.get(rowIndex);
            switch (colIndex) {
            case 0:
            	return rowName;
            case 1:
            	return sourceMap.get(rowName);
            case 2:
            	return sourceMap.getAbstract().contains(rowName);
            default:
            	return "";
            }
        }
    }

    @Override
	public void setValueAt(Object value, int rowIndex, int colIndex)
    {
        synchronized (sourceMap) {
            if (colIndex == 0) return;
            String rowName = sortedNameList.get(rowIndex);
            if (colIndex == 1) {
	            Class<? extends Object> oldElement = sourceMap.get(rowName).getClass();
	            // Correct incorrectly typed values - Booleans seem to be ok for now, but Integers started coming back as String in Java 7
	            if (oldElement == Double.class && value.getClass() == String.class)
	        		try {
	        			value = Double.valueOf((String)value);
	        		} catch (Exception ex) { }
	            if (oldElement == Integer.class && value.getClass() == String.class)
	        		try {
	        			value = Integer.valueOf((String)value);
	        		} catch (Exception ex) { }
	            if (value.getClass() != oldElement)
	            	JOptionPane.showMessageDialog(null, "This property should contain a "+oldElement.getName()+" not a "+value.getClass().getName(), "Incorrect datatype", JOptionPane.ERROR_MESSAGE);
	            else
	            	sourceMap.put(rowName, value);
            }
            else if (colIndex == 2) {
            	Boolean boolVal = (Boolean)value;
            	if (boolVal)
            		sourceMap.getAbstract().add(rowName);
            	else
            		sourceMap.getAbstract().remove(rowName);
            }
        }
        fireTableCellUpdated(rowIndex, colIndex);
    }

    @Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 2:
			return Boolean.class;
		default:
			return String.class;
		}
	}

	public void setMap(CastorHashMap props) {
        synchronized (sourceMap) {
            sourceMap = props;
            sortedNameList = new ArrayList<String>(props.size());
            for (String string : props.keySet())
				sortedNameList.add(string);

            Collections.sort(sortedNameList, new Comparator<String>() {
                @Override
				public int compare(String o1, String o2) {
                    return (o1.compareToIgnoreCase(o2));
                }
            });
        }
        fireTableChanged(new TableModelEvent(this));
    }

    @Override
	public boolean isCellEditable(int row, int col)
    {
        return col>0 && isEditable;
    }

    /**
     * @return Returns the isEditable.
     */
    public boolean isEditable() {
        return isEditable;
    }
    /**
     * @param isEditable The isEditable to set.
     */
    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }

    /**
     * @param text
     * @param object
     */
    public void addProperty(String text, Object object, boolean isAbstract) {
        sourceMap.put(text, object, isAbstract);
        setMap(sourceMap);
    }

    /**
     * @param object
     */
    public void delProperty(Object propName) {
        sourceMap.remove(propName);
        setMap(sourceMap);
    }
}
