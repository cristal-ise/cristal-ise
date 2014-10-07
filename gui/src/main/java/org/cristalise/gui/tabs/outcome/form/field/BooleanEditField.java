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
package org.cristalise.gui.tabs.outcome.form.field;

import java.awt.Component;
import java.awt.event.FocusEvent;

import javax.swing.JCheckBox;
import javax.swing.text.JTextComponent;

import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * $Revision: 1.7 $
 * $Date: 2005/08/16 13:59:56 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/
public class BooleanEditField extends StringEditField {

    JCheckBox checkbox;

    public BooleanEditField() {
        checkbox = new JCheckBox();
        checkbox.setSelected(false);
        checkbox.addFocusListener(this);
    }

    @Override
	public String getText() {
        return String.valueOf(checkbox.isSelected());
    }

    @Override
	public void setText(String text) {
        boolean newState = false;
        try {
            newState = Boolean.valueOf(text).booleanValue();
        } catch (Exception ex) {
            Logger.error("Invalid value for checkbox: "+text);
        }
        checkbox.setSelected(newState);
    }

    @Override
	public void setEditable(boolean editable) {
		super.setEditable(editable);
        checkbox.setEnabled(editable);
    }

    @Override
	public Component getControl() {
        return checkbox;
    }

    @Override
	public String getDefaultValue() {
        return "false";
    }

    /** don't reserve the item finder for a boolean */
    @Override
	public void focusGained(FocusEvent e) {
        helpPane.setHelp(name, helpText);
    }

    /**
     *
     */
    @Override
	public JTextComponent makeTextField() {
        // not used by boolean
        return null;
    }
}
