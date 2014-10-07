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
package org.cristalise.gui.tabs.outcome;

import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.PlainDocument;

import org.cristalise.kernel.utils.FileStringUtility;


/**************************************************************************
 *
 * $Revision: 1.4 $
 * $Date: 2005/09/07 13:46:31 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/



public class BasicOutcomeEditor extends JPanel implements OutcomeHandler {

    PlainDocument doc;
    JTextArea textarea;
    boolean unsaved;

    public BasicOutcomeEditor() {
        super();
        this.setLayout(new GridLayout(1,1));
        doc = new PlainDocument();
        textarea = new JTextArea(doc);
        textarea.setTabSize(2);
        textarea.setFont(Font.decode("monospaced"));
        add(new JScrollPane(textarea));
        doc.addDocumentListener(new DocumentListener() {
            @Override
			public void changedUpdate(DocumentEvent e) { unsaved = true; }
            @Override
			public void insertUpdate(DocumentEvent e) { unsaved = true; }
            @Override
			public void removeUpdate(DocumentEvent e) { unsaved = true; }

        });
    }

    @Override
	public void setOutcome(String outcome) throws InvalidOutcomeException {
        try {
            doc.insertString(0, outcome, null);
            unsaved = false;
        } catch (Exception ex) {
            throw new InvalidOutcomeException(ex.getMessage());
        }
    }

    @Override
	public void setDescription(String description) throws InvalidSchemaException { }

    @Override
	public void setReadOnly(boolean readOnly) {
        textarea.setEditable(!readOnly);
    }


    @Override
	public JPanel getPanel() throws OutcomeNotInitialisedException {
        return this;
    }

    /**
     *
     */

    @Override
	public String getOutcome() throws OutcomeException {
    	if (doc.getLength()==0) return null;
        try {
            return doc.getText(0, doc.getLength());
        } catch (Exception ex) {
            throw new OutcomeException(ex.getMessage());
        }
    }

    /**
     *
     */

    @Override
	public void run() {
    }


    @Override
	public boolean isUnsaved() {
        return unsaved;
    }

    @Override
	public void saved() {
        unsaved = false;
    }

	@Override
	public void export(File targetFile) throws Exception {
		FileStringUtility.string2File(targetFile, getOutcome());
	}
}
