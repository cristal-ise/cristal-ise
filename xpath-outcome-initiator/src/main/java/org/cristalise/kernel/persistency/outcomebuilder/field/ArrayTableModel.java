/**
 * This file is part of the CRISTAL-iSE XPath Outcome Initiator module.
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
package org.cristalise.kernel.persistency.outcomebuilder.field;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.cristalise.kernel.persistency.outcomebuilder.OutcomeStructure;
import org.exolab.castor.xml.schema.SimpleType;

public class ArrayTableModel {

    ArrayList<Object> contents = new ArrayList<Object>();
    Class<?> type;
    int numCols = 1;

    public ArrayTableModel(SimpleType type) {
        super();
        this.type = OutcomeStructure.getJavaClass(type.getTypeCode());
    }

    public void setData(String data) {
        contents.clear();
        StringTokenizer tok = new StringTokenizer(data);
        while(tok.hasMoreTokens())
            contents.add(OutcomeStructure.getTypedValue(tok.nextToken(), type));
    }

    public String getData() {
        if (contents.size() == 0) return "";
        Iterator<Object> iter = contents.iterator();
        StringBuffer result = new StringBuffer(iter.next().toString());
        while (iter.hasNext())
            result.append(" ").append(iter.next().toString());
        return result.toString();
    }

    public void addField() {
        contents.add(OutcomeStructure.getTypedValue("", type));
    }

    public void removeField() {
        contents.remove(contents.size()-1);
    }

    public Class<?> getColumnClass(int columnIndex) {
        return type;
    }

    public int getColumnCount() {
        return numCols;
    }

    public int getArrayLength() {
        return contents.size();
    }

    public void setColumnCount(int newCols) {
        numCols = newCols;
    }

    public String getColumnName(int column) {
        return "Value";
    }

    public int getRowCount() {
        return (contents.size()/numCols)+1;
    }

    public Object getValueAt(int arg0, int arg1) {
        int index = arg1+(arg0 * numCols);
        if (index >= contents.size())
            return null;
        return contents.get(arg1+(arg0 * numCols));
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        contents.set(columnIndex+(rowIndex*numCols), aValue);
    }
}
