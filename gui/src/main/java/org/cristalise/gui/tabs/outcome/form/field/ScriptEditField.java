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

import java.awt.Component;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class ScriptEditField extends EditField {

	RSyntaxTextArea scriptTextArea;
	RTextScrollPane scriptScroll;
	RSyntaxDocument scriptDoc;
	
	public ScriptEditField() {
		scriptDoc = new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
		
		scriptTextArea = new RSyntaxTextArea(scriptDoc);
		scriptTextArea.setAutoIndentEnabled(true);
		scriptTextArea.setAnimateBracketMatching(true);
		scriptTextArea.setCodeFoldingEnabled(true);

		scriptScroll = new RTextScrollPane(scriptTextArea);
		scriptScroll.setLineNumbersEnabled(true);  
		
	}
    @Override
	public String getText() {
        try {
			return scriptDoc.getText(0, scriptDoc.getLength());
		} catch (BadLocationException e) {
			return "";
		}
    }

    @Override
	public void setText(String text) {
    	scriptDoc = new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
		try {
			scriptDoc.insertString(0, text, null);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		scriptTextArea.setDocument(scriptDoc);
    }

    @Override
	public void setEditable(boolean editable) {
    	scriptTextArea.setEditable(editable);
	}
    
	@Override
	public String getDefaultValue() {
        return "";
    }

	public Component getControl() {
        return scriptScroll;
    }
	
    @Override
	public JTextComponent makeTextField() {
        // not used by this
    	return null;
    }
    
}
