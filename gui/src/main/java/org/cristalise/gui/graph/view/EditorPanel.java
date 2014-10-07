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
package org.cristalise.gui.graph.view;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.cristalise.gui.graph.controller.AutoScrollController;
import org.cristalise.gui.graph.controller.EdgeConstructionController;
import org.cristalise.gui.graph.controller.MultiSelectionDragController;
import org.cristalise.gui.graph.controller.VertexConstructionController;
import org.cristalise.kernel.graph.model.EdgeFactory;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.TypeNameAndConstructionInfo;
import org.cristalise.kernel.graph.model.VertexFactory;
import org.cristalise.kernel.graph.model.VertexOutlineCreator;

public class EditorPanel extends JPanel
{
	// Graph Model
	public final GraphModelManager mGraphModelManager = new GraphModelManager();
	// Graph View
	public GraphPanel mGraphPanel = null;
    protected JScrollPane mGraphScrollPane = null;
	// Graph Controllers
	protected MultiSelectionDragController mMultiSelectionDragController;
    protected VertexConstructionController mVertexConstructionController = new VertexConstructionController();
    protected EdgeConstructionController mEdgeConstructionController = new EdgeConstructionController();
    protected AutoScrollController mAutoScrollController = new AutoScrollController();
	// Tool bar
    protected EditorToolBar mEditorToolBar = null;
	protected EditorPanel()
    {
    }
    public EditorPanel(EdgeFactory eFactory, VertexFactory vFactory, VertexOutlineCreator vOutlineCreator, boolean edgeCreationMode,	// True
																																		// if
																																		// edges
																																		// can
																																		// be
																																		// created
	JButton[] otherButtons, GraphPanel graphPanel)
	{
		// Create the graph panel and editor tool bar
        setDoubleBuffered(true);
		mGraphPanel = graphPanel;
		mGraphPanel.setGraphModelManager(mGraphModelManager);
		mGraphScrollPane = new JScrollPane(mGraphPanel);
		mGraphModelManager.setExternalEdgeFactory(eFactory);
		mGraphModelManager.setExternalVertexFactory(vFactory);
		mGraphModelManager.setVertexOutlineCreator(vOutlineCreator);
		mEditorToolBar = new EditorToolBar(edgeCreationMode, otherButtons, graphPanel);
		mEditorToolBar.setGraphModelManager(mGraphModelManager);
		mEditorToolBar.setGraphPanel(mGraphPanel);
		createLayout();
		// The graph panel observes the graph model
		mGraphModelManager.addObserver(mGraphPanel);
		// The multi selection drag controller modifies the graph model
		// and listens to the graph panel and editor tool bar
		mMultiSelectionDragController = new MultiSelectionDragController(mGraphModelManager, mGraphPanel);
		mGraphPanel.addMouseListener(mMultiSelectionDragController);
		mGraphPanel.addMouseMotionListener(mMultiSelectionDragController);
		mGraphPanel.addKeyListener(mMultiSelectionDragController);
		mEditorToolBar.addEditorModeListener(mMultiSelectionDragController);
		// The edge construction controller modifies the graph model, queries the graph panel
		// and listens to the graph panel and editor tool bar
		mEdgeConstructionController.setGraphModelManager(mGraphModelManager);
		mEdgeConstructionController.setGraphPanel(mGraphPanel);
		mGraphPanel.addMouseListener(mEdgeConstructionController);
		mGraphPanel.addMouseMotionListener(mEdgeConstructionController);
		mEdgeConstructionController.setEditorToolBar(mEditorToolBar);
		// The vertex construction controller modifies the graph model
		// and listens to the graph panel and editor tool bar
		mVertexConstructionController.setGraphModelManager(mGraphModelManager);
		mGraphPanel.addMouseListener(mVertexConstructionController);
		mVertexConstructionController.setEditorToolBar(mEditorToolBar);
		// The auto scroll controller listens to and modifies the
		// graph panel
		mAutoScrollController.setGraphPanel(mGraphPanel);
	}

    protected void createLayout()
	{
		setLayout(new BorderLayout());
		add(mEditorToolBar, BorderLayout.NORTH);
		mGraphPanel.setPreferredSize(new Dimension(mGraphModelManager.getModel().getWidth(), mGraphModelManager.getModel().getHeight()));
		add(mGraphScrollPane, BorderLayout.CENTER);
	}
	public void enterSelectMode()
	{
		mEditorToolBar.enterSelectMode();
	}
	public void updateEdgeTypes(TypeNameAndConstructionInfo[] typeNameAndConstructionInfo)
	{
		mEditorToolBar.updateEdgeTypes(typeNameAndConstructionInfo);
	}
	public void updateVertexTypes(TypeNameAndConstructionInfo[] typeNameAndConstructionInfo)
	{
		mEditorToolBar.updateVertexTypes(typeNameAndConstructionInfo);
	}
	public void setEditable(boolean editable)
	{
		mGraphModelManager.setEditable(editable);
		mEditorToolBar.setGraphEditable(editable);
	}
}
