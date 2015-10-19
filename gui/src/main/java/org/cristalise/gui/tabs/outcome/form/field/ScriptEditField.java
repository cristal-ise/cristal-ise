package org.cristalise.gui.tabs.outcome.form.field;

import java.awt.Component;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.cristalise.kernel.scripting.ParameterException;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptParsingException;
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
