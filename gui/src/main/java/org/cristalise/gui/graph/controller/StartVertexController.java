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
package org.cristalise.gui.graph.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;

import org.cristalise.gui.graph.event.SelectionChangedEvent;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.Vertex;



// The start vertex controller is responsible for selecting
// the vertex at the start of the graph.
//
// The controller listens to:
//     * The graph model to determine if there is a single
//       vertex selected
//     * The start vertex button
//
// The controller modifies:
//     * The graph model to select the start vertex
//     * The start button to enable it only when there is a
//       single vertex selected
public class StartVertexController implements Observer, ActionListener
{
    private GraphModelManager mGraphModelManager  = null;
    private JButton           mStartButton        = null;
    private Vertex 			  selectedVertex	  = null;


    public void setGraphModelManager(GraphModelManager graphModelManager)
    {
        mGraphModelManager = graphModelManager;
        mGraphModelManager.addObserver(this);
    }


    public void setStartButton(JButton startButton)
    {
        mStartButton = startButton;
        mStartButton.addActionListener(this);
    }


    @Override
	public void update(Observable o, Object arg)
    {
        SelectionChangedEvent event            = null;
        Vertex[]              selectedVertices = null;

        // If the selected vertex has changed
        if(arg instanceof SelectionChangedEvent && mStartButton != null)
        {
            event            = (SelectionChangedEvent)arg;
            selectedVertices = event.mSelection.mVertices;

            if(selectedVertices == null || selectedVertices.length != 1)
            {
                mStartButton.setEnabled(false);
            }
            else if (mGraphModelManager.isEditable())
            {
                mStartButton.setEnabled(true);
                selectedVertex = selectedVertices[0];
            }
        }
    }


    @Override
	public void actionPerformed(ActionEvent ae)
    {
        if(selectedVertex != null)
        {
            mGraphModelManager.getModel().setStartVertexId(selectedVertex.getID());
        }
    }
}
