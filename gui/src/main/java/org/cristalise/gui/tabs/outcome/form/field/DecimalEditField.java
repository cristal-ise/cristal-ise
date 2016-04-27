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
package org.cristalise.gui.tabs.outcome.form.field;

import java.awt.Toolkit;
import java.math.BigDecimal;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**************************************************************************
 *
 * $Revision: 1.3 $
 * $Date: 2005/08/16 13:59:56 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/
public class DecimalEditField extends EditField {

    public DecimalEditField() {
        super();
        field.addFocusListener(this);
        field.setToolTipText("This field must contains a decimal number e.g. 3.14159265");
    }

    @Override
	public String getText() {
        return field.getText();
    }

    @Override
	public void setText(String text) {
        field.setText(text);
    }

    @Override
	public String getDefaultValue() {
        return "0.0";
    }

    @Override
	public JTextComponent makeTextField() {
        return new DecimalTextField();
    }

    private class DecimalTextField extends JTextField {

        public DecimalTextField() {
            super();
            setHorizontalAlignment(RIGHT);
        }
        @Override
		protected Document createDefaultModel() {
            return new Decimal();
        }
    }

    private class Decimal extends PlainDocument {

        @Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

            if (str == null || str.equals("")) {
                return;
            }

            String proposedResult = null;

            if (getLength() == 0) {
                proposedResult = str;
            } else {
                StringBuffer currentBuffer = new StringBuffer( this.getText(0, getLength()) );
                currentBuffer.insert(offs, str);
                proposedResult = currentBuffer.toString();
            }

            try {
                parse(proposedResult);
                super.insertString(offs, str, a);
            } catch (Exception e) {
                Toolkit.getDefaultToolkit().beep();
            }

        }

        @Override
		public void remove(int offs, int len) throws BadLocationException {

            String currentText = this.getText(0, getLength());
            String beforeOffset = currentText.substring(0, offs);
            String afterOffset = currentText.substring(len + offs, currentText.length());
            String proposedResult = beforeOffset + afterOffset;

            if (proposedResult.length() == 0) { // empty is ok
                super.remove(offs, len);
                return;
            }
            try {
                parse(proposedResult);
                super.remove(offs, len);
            } catch (Exception e) {
                Toolkit.getDefaultToolkit().beep();
            }

        }

        public BigDecimal parse(String proposedResult) throws NumberFormatException {

            BigDecimal value = new BigDecimal(0);
            if ( proposedResult.length() != 0) {
                value =  new BigDecimal(proposedResult);
            }
            return value;
        }
    }
}
