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
package org.cristalise.gui.lifecycle.instance;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.lifecycle.chooser.ActivityChooser;
import org.cristalise.gui.lifecycle.chooser.WorkflowDialogue;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.TypeNameAndConstructionInfo;
import org.cristalise.kernel.graph.model.VertexFactory;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.utils.LocalObjectLoader;

public class WfVertexFactory implements VertexFactory, WorkflowDialogue
{
	protected CompositeActivity mRootAct = null;
	@Override
	public void create(GraphModelManager graphModelManager, GraphPoint location, TypeNameAndConstructionInfo typeNameAndConstructionInfo)
	{
		String vertexTypeId = null;
		if (mRootAct != null && typeNameAndConstructionInfo.mInfo instanceof String)
		{
			vertexTypeId = (String) typeNameAndConstructionInfo.mInfo;
			if (vertexTypeId.equals("Atomic") || vertexTypeId.equals("Composite"))
			{
				HashMap<String, Object> mhm = new HashMap<String, Object>();
				mhm.put("P1", vertexTypeId);
				mhm.put("P2", location);
				//************************************************
				ActivityChooser a =
					new ActivityChooser(
						"Please enter a Type for the new activity",
						"New " + vertexTypeId + " Activity",
						ImageLoader.findImage("graph/newvertex_large.png").getImage(),
						this,
						mhm);
				a.setVisible(true);
			}
			else
				mRootAct.newChild(vertexTypeId, location);
		}
	}
	@Override
	public void setCreationContext(Object newContext)
	{
		if (newContext != null && newContext instanceof CompositeActivity)
			mRootAct = (CompositeActivity) newContext;
	}
	@Override
	public void loadThisWorkflow(String newName, HashMap<String, Object> hashMap)
	{
		String vertexTypeId = (String) hashMap.get("P1");
		GraphPoint location = (GraphPoint) hashMap.get("P2");
		if (newName == null)
			return;


		String unicName = newName;
		while (mRootAct.search(mRootAct.getPath() + "/" + unicName) != null)
		{
			unicName =
				(String) JOptionPane.showInputDialog(
					null,
					"Activity name not unique. Please give another.",
					"New " + vertexTypeId + " Activity",
					JOptionPane.QUESTION_MESSAGE,
					ImageLoader.findImage("graph/newvertex_large.png"),
					null,
					null);
			if (newName.equals(""))
				return;
		}
		Activity act = null;
		try
		{
			ActivityDef actD = LocalObjectLoader.getActDef(newName, 0);
			act = (Activity)actD.instantiate(unicName);
		}
		catch (Exception e)
		{
		}
		if (act == null)
			mRootAct.newChild(unicName, vertexTypeId, location);
		else
			mRootAct.newExistingChild(act, unicName, location);
	}
}
