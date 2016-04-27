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
package org.cristalise.gui.tabs.outcome;

import java.awt.GridLayout;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.cristalise.kernel.utils.FileStringUtility;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;


/**************************************************************************
 *
 * $Revision: 1.4 $
 * $Date: 2005/09/07 13:46:31 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/



public class BasicOutcomeEditor extends JPanel implements OutcomeHandler {

	RSyntaxTextArea textarea;
	RSyntaxDocument doc;
    boolean unsaved;

    public BasicOutcomeEditor() {
        super();
        this.setLayout(new GridLayout(1,1));
        doc = new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_XML);
        textarea = new RSyntaxTextArea(doc);
        textarea.setAutoIndentEnabled(true);
        textarea.setCodeFoldingEnabled(true);
        RTextScrollPane scroll = new RTextScrollPane(textarea);
		scroll.setLineNumbersEnabled(true);
		add(scroll);
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
        revalidate();
        doLayout();
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
