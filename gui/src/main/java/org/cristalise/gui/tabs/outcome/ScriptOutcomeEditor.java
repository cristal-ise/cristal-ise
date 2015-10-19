package org.cristalise.gui.tabs.outcome;

import org.cristalise.gui.tabs.outcome.form.OutcomePanel;
import org.cristalise.gui.tabs.outcome.form.field.ScriptEditField;

public class ScriptOutcomeEditor extends OutcomePanel {
	
	public ScriptOutcomeEditor() {
        super();
        specialEditFields.put("script", ScriptEditField.class);
	}
}
