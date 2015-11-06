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
package org.cristalise.gui.lifecycle.desc;

import java.io.BufferedWriter;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.cristalise.gui.graph.view.VertexPropertyPanel;
import org.cristalise.gui.tabs.outcome.InvalidOutcomeException;
import org.cristalise.gui.tabs.outcome.InvalidSchemaException;
import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.gui.tabs.outcome.OutcomeHandler;
import org.cristalise.gui.tabs.outcome.OutcomeNotInitialisedException;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.ActivitySlotDef;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * $Revision: 1.5 $
 * $Date: 2005/10/05 07:39:37 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

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
            Logger.error(ex);
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
            Logger.error(ex);
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
		exportAct(targetFile.getParentFile(), null, act);
	}

	public static void exportAct(File dir, BufferedWriter imports, ActivityDef actDef) throws Exception {
		
		// Export associated schema
		exportSchema((String)actDef.getProperties().get("SchemaType"), actDef.getProperties().get("SchemaVersion"), imports, new File(dir, "OD"));
		// Export associated script
		exportScript((String)actDef.getProperties().get("ScriptName"), actDef.getProperties().get("ScriptVersion"), imports, new File(dir, "SC"));

		//Export child act if composite
		if (actDef instanceof CompositeActivityDef) {
			CompositeActivityDef compActDef = (CompositeActivityDef)actDef;
			for (int i=0; i<compActDef.getChildren().length; i++) { // export slot defined scripts and schemas
				GraphableVertex vert = compActDef.getChildren()[i];
				exportScript((String)vert.getProperties().get("ScriptName"), actDef.getProperties().get("ScriptVersion"), imports, new File(dir, "SC"));
				exportScript((String)vert.getProperties().get("RoutingScriptName"), actDef.getProperties().get("RoutingScriptVersion"), imports, new File(dir, "SC"));
				exportSchema((String)vert.getProperties().get("SchemaType"), actDef.getProperties().get("SchemaVersion"), imports, new File(dir, "OD"));
			}
			GraphableVertex[] childDefs = compActDef.getLayoutableChildren();
			for (GraphableVertex childDef : childDefs) {
				if (childDef instanceof ActivitySlotDef) {
					if ("last".equals(childDef.getProperties().get("Version"))) {
						throw new Exception("Version set to 'last' for Activity '"+childDef.getName()+"' in "+actDef.getActName());
					}
					exportAct(dir, imports, ((ActivitySlotDef)childDef).getTheActivityDef());
				}
			}
			// export marshalled compAct
			FileStringUtility.string2File(new File(new File(dir, "CA"), compActDef.getActName()+".xml"), Gateway.getMarshaller().marshall(compActDef));
			if (imports!=null) {
				imports.write("<Resource name=\""+compActDef.getActName()+"\" "+(compActDef.getVersion()==-1?"":"version=\""+compActDef.getVersion()+"\" ")+"type=\"CA\">boot/CA/"+compActDef.getActName()+".xml</Resource>\n");
			}
		}
		else {
			FileStringUtility.string2File(new File(new File(dir, "EA"), actDef.getActName()+".xml"), Gateway.getMarshaller().marshall(actDef));
			if (imports!=null) imports.write("<Resource name=\""+actDef.getActName()+"\" "+(actDef.getVersion()==-1?"":"version=\""+actDef.getVersion()+"\" ")+"type=\"EA\">boot/EA/"+actDef.getActName()+".xml</Resource>\n");
		}
	}

	public static void exportScript(String name, Object version, BufferedWriter imports, File dir) {
		if (name == null || name.length()==0 || name.contains(":")) return;
		try {
			int intVersion;
			if (version instanceof String) intVersion = Integer.parseInt((String)version);
			else if (version instanceof Integer) intVersion = ((Integer)version).intValue();
			else return;

			FileStringUtility.string2File(new File(dir, name+".xml"),
			LocalObjectLoader.getScript(name, intVersion).getScriptData());
			if (imports!=null) imports.write("<Resource name=\""+name+"\" "+(version==null?"":"version=\""+version+"\" ")+"type=\"SC\">boot/SC/"+name+".xml</Resource>\n");
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(null, "Invalid version number in script version:"+version);
		} catch (Exception ex) {
			Logger.error(ex);
			JOptionPane.showMessageDialog(null, "Could not export script "+name+"_"+version, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public static void exportSchema(String name, Object version, BufferedWriter imports, File dir) {
		if (name == null || name.length()==0) return;
		try {
			int intVersion;
			if (version instanceof String) intVersion = Integer.parseInt((String)version);
			else if (version instanceof Integer) intVersion = ((Integer)version).intValue();
			else return;
			FileStringUtility.string2File(new File(dir, name+".xsd"),
					LocalObjectLoader.getSchema(name, intVersion).getSchemaData());
			if (imports!=null) imports.write("<Resource name=\""+name+"\" "+(version==null?"":"version=\""+version+"\" ")+"type=\"OD\">boot/OD/"+name+".xsd</Resource>\n");
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(null, "Invalid version number in script version:"+version);
		} catch (Exception ex) {
			Logger.error(ex);
			JOptionPane.showMessageDialog(null, "Could not export schema "+name+"_"+version, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
