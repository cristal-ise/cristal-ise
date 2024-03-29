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

import java.util.HashMap;

import javax.swing.JOptionPane;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.lifecycle.chooser.ActivityChooser;
import org.cristalise.gui.lifecycle.chooser.WorkflowDialogue;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.TypeNameAndConstructionInfo;
import org.cristalise.kernel.graph.model.VertexFactory;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.WfVertexDef;
import org.cristalise.kernel.utils.LocalObjectLoader;

public class WfVertexDefFactory implements VertexFactory, WorkflowDialogue {
    protected CompositeActivityDef mCompositeActivityDef = null;

    @Override
    public void create(GraphModelManager graphModelManager, GraphPoint location,
            TypeNameAndConstructionInfo typeNameAndConstructionInfo)
            throws ObjectNotFoundException, InvalidDataException {
        String vertexTypeId = null;
        if (mCompositeActivityDef != null && typeNameAndConstructionInfo.mInfo instanceof String) {
            vertexTypeId = (String) typeNameAndConstructionInfo.mInfo;
            if (vertexTypeId.equals("Atomic") || vertexTypeId.equals("Composite")) {
                // ask for a name
                HashMap<String, Object> mhm = new HashMap<String, Object>();
                mhm.put("P1", vertexTypeId);
                mhm.put("P2", location);
                // ************************************************
                ActivityChooser a = new ActivityChooser(vertexTypeId,
                        ImageLoader.findImage("graph/newvertex_large.png").getImage(), this, mhm);
                a.setVisible(true);
            } else {
                String localName = "";
                if (vertexTypeId.equals("AtomicLocal") || vertexTypeId.equals("CompositeLocal"))
                    localName = promptName(vertexTypeId);
                mCompositeActivityDef.newChild(localName, vertexTypeId, null, location);
            }
        }
    }

    @Override
    public void loadThisWorkflow(String newName, Integer version, HashMap<String, Object> hashMap)
            throws ObjectNotFoundException, InvalidDataException {
        String vertexTypeId = (String) hashMap.get("P1");
        GraphPoint location = (GraphPoint) hashMap.get("P2");

        if (newName == null || newName.equals("")) return;

        // Resolve activity def
        ActivityDef act;
        try {
            act = LocalObjectLoader.getActDef(newName, version == null ? 0 : version);
        } catch (Exception ex) {
            MainFrame.exceptionDialog(ex);
            return;
        }

        // Check if it's already in this wf
        WfVertexDef slot = (WfVertexDef) mCompositeActivityDef
                .search(mCompositeActivityDef.getID() + "/" + newName);
        if (slot != null) {
            do {
                newName = promptName(vertexTypeId);

            } while (newName == null || newName.length() == 0 || mCompositeActivityDef
                    .search(mCompositeActivityDef.getID() + "/" + newName) != null);
            slot = mCompositeActivityDef.addExistingActivityDef(newName, act, location);
        } else {
            slot = mCompositeActivityDef.newChild(newName, vertexTypeId, version, location);
        }

        // Create all abstract properties in the new slot
        for (String propName : act.getProperties().getAbstract()) {
            slot.getProperties().put(propName, act.getProperties().get(propName));
        }
    }

    @Override
    public void setCreationContext(Object newContext) {
        if (newContext != null && newContext instanceof CompositeActivityDef)
            mCompositeActivityDef = (CompositeActivityDef) newContext;
    }

    public String promptName(String type) {
        return (String) JOptionPane.showInputDialog(null,
                "Please provide a unique name for this instance of the activity",
                "New " + type + " Activity Instance", JOptionPane.QUESTION_MESSAGE,
                ImageLoader.findImage("graph/newvertex_large.png"), null, null);
    }
}
