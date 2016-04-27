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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;

import org.cristalise.gui.graph.event.SelectionChangedEvent;
import org.cristalise.gui.graph.view.GraphPanel;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.Vertex;



// The deletion controller is responsible for deleting the present
// selection within the graph.
//
// The controller listens to:
//     * The graph model to determine if there is a selection
//     * The delete button
//     * The graph panel for the typing of the delete key
//
// The controller modifies:
//     * The graph model to delete the current selection
//     * The delete button to enable it only when there is a selection
public class DeletionController extends KeyAdapter implements Observer, ActionListener
{
    private GraphModelManager mGraphModelManager   = null;
    private GraphPanel		  mGraphPanel          = null;
    private JButton           mDeleteButton        = null;


    public void setGraphModelManager(GraphModelManager graphModelManager)
    {
        mGraphModelManager = graphModelManager;
        mGraphModelManager.addObserver(this);
    }
    
    public void setGraphPanel(GraphPanel graphPanel)
    {
        mGraphPanel = graphPanel;
    }


    public void setDeleteButton(JButton deleteButton)
    {
        mDeleteButton = deleteButton;
        mDeleteButton.addActionListener(this);
    }


    // Invoked by the graph model
    @Override
	public void update(Observable o, Object arg)
    {
        SelectionChangedEvent event            = null;
        DirectedEdge          selectedEdge     = null;
        Vertex[]              selectedVertices = null;


        // If the selected edge has changed
        if(arg instanceof SelectionChangedEvent && mDeleteButton != null  && mGraphModelManager.isEditable())
        {
            // Enable the button if a single edge or single vertex is selected
            event            = (SelectionChangedEvent)arg;

            selectedEdge     = event.mSelection.mEdge;
            selectedVertices = event.mSelection.mVertices;
            mDeleteButton.setEnabled(selectedEdge != null || selectedVertices != null);
        }
    }


    // Invoked by the graph panel
    @Override
	public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_DELETE && mGraphModelManager.isEditable())
        {
        	mGraphPanel.deleteSelection();
        }
    }


    // Invoked by the delete button
    @Override
	public void actionPerformed(ActionEvent ae)
    {
        if(mGraphModelManager != null && mGraphModelManager.isEditable())
        {
        	mGraphPanel.deleteSelection();
        }
    }
}
