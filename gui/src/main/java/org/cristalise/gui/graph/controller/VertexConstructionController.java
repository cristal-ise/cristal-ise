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
package org.cristalise.gui.graph.controller;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.graph.view.EditorModeListener;
import org.cristalise.gui.graph.view.EditorToolBar;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.GraphPoint;



public class VertexConstructionController extends MouseAdapter implements EditorModeListener
{
    private GraphModelManager mGraphModelManager = null;
    private EditorToolBar     mEditorToolBar     = null;
    private boolean           mCreatingVertices  = false;


    public void setGraphModelManager(GraphModelManager graphModelManager)
    {
        mGraphModelManager = graphModelManager;
    }


    public void setEditorToolBar(EditorToolBar editorToolBar)
    {
        mEditorToolBar = editorToolBar;
        mEditorToolBar.addEditorModeListener(this);
    }


    @Override
	public void editorModeChanged(String idOfNewMode)
    {
        mCreatingVertices = idOfNewMode.equals("Vertex");
    }


    @Override
	public void mouseClicked(MouseEvent me)
    {
        if(mCreatingVertices && (mGraphModelManager != null) && (mEditorToolBar != null) && mGraphModelManager.isEditable())
        {
            try {
				mGraphModelManager.getModel().createVertex(new GraphPoint(me.getPoint().x, me.getPoint().y), mEditorToolBar.getSelectedVertexType());
			} catch (Exception e) {
				MainFrame.exceptionDialog(e);
			}
            mEditorToolBar.enterSelectMode();
        }
    }
}
