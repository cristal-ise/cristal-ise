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
package org.cristalise.gui.lifecycle.desc;

import java.io.File;

import javax.swing.JPanel;

import org.cristalise.gui.graph.view.VertexPropertyPanel;
import org.cristalise.gui.tabs.outcome.InvalidOutcomeException;
import org.cristalise.gui.tabs.outcome.InvalidSchemaException;
import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.gui.tabs.outcome.OutcomeHandler;
import org.cristalise.gui.tabs.outcome.OutcomeNotInitialisedException;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.FileStringUtility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElemActDefOutcomeHandler extends VertexPropertyPanel implements OutcomeHandler {

    ActivityDef act;
    boolean unsaved;
    public ElemActDefOutcomeHandler() {
        super(true);
        createLayout(null);
    }

    /**
     *
     */
    @Override
	public void setOutcome(String outcome) throws InvalidOutcomeException {
        try {
            act = (ActivityDef)Gateway.getMarshaller().unmarshall(outcome);
            setVertex(act);
        } catch (Exception ex) {
            log.error("",ex);
            throw new InvalidOutcomeException();
        }
    }

    /**
     *
     */
    @Override
	public void setDescription(String description)
            throws InvalidSchemaException {
        // ignore
    }

    /**
     *
     */
    @Override
	public void setReadOnly(boolean readOnly) {
        setEditable(!readOnly);

    }

    /**
     *
     */
    @Override
	public JPanel getPanel() throws OutcomeNotInitialisedException {
        return this;
    }

    /**
     *
     */
    @Override
	public String getOutcome() throws OutcomeException {
        try {
            return Gateway.getMarshaller().marshall(act);
        } catch (Exception ex) {
            log.error("",ex);
            throw new OutcomeException();
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