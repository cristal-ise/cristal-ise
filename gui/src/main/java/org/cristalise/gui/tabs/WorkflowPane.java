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
package org.cristalise.gui.tabs;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JSplitPane;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.graph.controller.Selection;
import org.cristalise.gui.graph.view.EditorPanel;
import org.cristalise.gui.graph.view.VertexPropertyPanel;
import org.cristalise.gui.lifecycle.instance.TransitionPanel;
import org.cristalise.gui.lifecycle.instance.WfDirectedEdgeRenderer;
import org.cristalise.gui.lifecycle.instance.WfEdgeFactory;
import org.cristalise.gui.lifecycle.instance.WfGraphPanel;
import org.cristalise.gui.lifecycle.instance.WfVertexFactory;
import org.cristalise.gui.lifecycle.instance.WfVertexRenderer;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.graph.layout.DefaultGraphLayoutGenerator;
import org.cristalise.kernel.graph.model.EdgeFactory;
import org.cristalise.kernel.graph.model.VertexFactory;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.WfVertexOutlineCreator;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;

public class WorkflowPane extends ItemTabPane implements ProxyObserver<Workflow>
{
	// Only for the purpose of loading and saving
	protected Workflow mWorkflow = null;
	boolean init = false;
	TransitionPanel transPanel;
	protected JButton mLoadButton = new JButton(ImageLoader.findImage("graph/load.png"));
	protected JButton mSaveButton = new JButton(ImageLoader.findImage("graph/save.png"));
	protected JButton mLayoutButton = new JButton(ImageLoader.findImage("graph/autolayout.png"));
	protected JButton mZoomOutButton = new JButton(ImageLoader.findImage("graph/zoomout.png"));
	protected JButton[] mOtherToolBarButtons;
	// Workflow factories
	protected EdgeFactory mWfEdgeFactory;
	protected VertexFactory mWfVertexFactory;
	// Graph editor panel
	protected EditorPanel mEditorPanel;
	// Objects to view/modify the properties of the selected activity
	protected VertexPropertyPanel mPropertyPanel;
	protected JSplitPane mSplitPane;

	// Graph editor panel
	// Objects to view/modify the properties of the selected activity
	public WorkflowPane()
	{
		super("Workflow", "Workflow Viewer");
		// Workflow factories
		mWfEdgeFactory = new WfEdgeFactory();
		mWfVertexFactory = new WfVertexFactory();
		mZoomOutButton.setToolTipText("Zoom Out");
		mLayoutButton.setToolTipText("Auto Layout");
		mLoadButton.setToolTipText("Load");
		mSaveButton.setToolTipText("Save");
		mOtherToolBarButtons = new JButton[] { mZoomOutButton, mLayoutButton, mLoadButton, mSaveButton };

	}

	protected void createListeners()
	{
		/**
		 *
		 */
		mLoadButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				loadWorkflow();
			}
		});
		/**
		 *
		 */
		mSaveButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				setCursor(new Cursor(Cursor.WAIT_CURSOR));
				saveWorkflow();
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
		/**
		 *
		 */
		mLayoutButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				DefaultGraphLayoutGenerator.layoutGraph(mEditorPanel.mGraphModelManager.getModel());
			}
		});
		/**
		 *
		 */
		mZoomOutButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				mEditorPanel.mGraphModelManager.zoomOut();
			}
		});
	}
	/**
	 * Return a single ref on mEditorPanel
	 *
	 * @return EditorPanel
	 */
	public EditorPanel getEditorPanel()
	{
		if (mEditorPanel == null)
			mEditorPanel =
				new EditorPanel(
					mWfEdgeFactory,
					mWfVertexFactory,
					new WfVertexOutlineCreator(),
					true,
					mOtherToolBarButtons,
					new WfGraphPanel(new WfDirectedEdgeRenderer(), new WfVertexRenderer()));
		return mEditorPanel;
	}
	public JSplitPane getJSplitPane()
	{
		if (mSplitPane == null)
		{
			mSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getEditorPanel(), getPropertyPanel());
			mSplitPane.setDividerSize(5);
			mSplitPane.setDividerLocation(800);
		}
		return mSplitPane;
	}
	@Override
	public void add(Workflow contents)
	{
		mWorkflow = contents;
		addActivity((CompositeActivity) mWorkflow.search("workflow/domain"));
	}
	@Override
	public void remove(String id)
	{
	}
	protected void addActivity(CompositeActivity cAct)
	{
		// Resolve any undefined references in the workflow
		mEditorPanel.mGraphModelManager.replace(cAct.getChildrenGraphModel());
		// Give the editor panel the edge and vertex types
		mEditorPanel.updateVertexTypes(cAct.getWf().getVertexTypeNameAndConstructionInfo());
		mEditorPanel.updateEdgeTypes(cAct.getWf().getEdgeTypeNameAndConstructionInfo());
		mEditorPanel.enterSelectMode();
		mWfVertexFactory.setCreationContext(cAct);
	}
	@Override
	public void run()
	{
        Thread.currentThread().setName("Workflow Pane Builder");
		if (!init)
		{
			getEditorPanel();
			createLayout();
			createListeners();
			mPropertyPanel.setGraphModelManager(mEditorPanel.mGraphModelManager);
            mEditorPanel.setEditable(MainFrame.isAdmin);
			init = true;
		}
		sourceItem.getItem().subscribe(new MemberSubscription<Workflow>(this, ClusterStorage.LIFECYCLE, true));
		transPanel.setItem(sourceItem.getItem());
	}
	@Override
	public void reload()
	{
		Gateway.getStorage().clearCache(sourceItem.getItemPath(), ClusterStorage.LIFECYCLE);
		initForItem(sourceItem);
	}
    protected void createLayout()
    {
        initPanel();
        // Add the editor pane
        getGridBagConstraints().gridx = 0;
        getGridBagConstraints().gridy = 1;
        getGridBagConstraints().fill = GridBagConstraints.BOTH;
        getGridBagConstraints().weighty = 2.0;
        gridbag.setConstraints(getJSplitPane(), getGridBagConstraints());
        add(getJSplitPane());
        validate();
    }
	protected void loadWorkflow()
	{
		File selectedFile = null;
		int returnValue = MainFrame.xmlChooser.showOpenDialog(null);
		switch (returnValue)
		{
			case JFileChooser.APPROVE_OPTION :
				selectedFile = MainFrame.xmlChooser.getSelectedFile();
				try
				{
					String newWf = FileStringUtility.file2String(selectedFile);
					add((Workflow) Gateway.getMarshaller().unmarshall(newWf));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			case JFileChooser.CANCEL_OPTION :
			case JFileChooser.ERROR_OPTION :
			default :
				}
	}
	protected void saveWorkflow()
	{
		try
		{
			CompositeActivity cact = (CompositeActivity)mWorkflow.getWf().search("workflow/domain");
            MainFrame.userAgent.execute(
            	sourceItem.getItem(),
				"ReplaceDomainWorkflow",
				new String[] { Gateway.getMarshaller().marshall(cact)});
            mEditorPanel.mGraphPanel.setSelection(new Selection(null,null,0,0,0,0));
		}
		catch (Exception e)
		{
			Logger.error(e);
		}
	}

    public VertexPropertyPanel getPropertyPanel()
    {
        if (mPropertyPanel == null) {
            setNewPropertyPanel();
            transPanel = new TransitionPanel();
            mPropertyPanel.createLayout(transPanel);
            mPropertyPanel.setGraphModelManager(mEditorPanel.mGraphModelManager);
            mPropertyPanel.setEditable(MainFrame.isAdmin);
        }
        return mPropertyPanel;
    }

	public void setNewPropertyPanel()
	{
		String wfPanelClass = Gateway.getProperties().getProperty("WfPropertyPanel");
		if (wfPanelClass != null)
		{
			try
			{
				Class<?> panelClass = Class.forName(wfPanelClass);
				mPropertyPanel = (VertexPropertyPanel) panelClass.newInstance();
				return;
			}
			catch (Exception ex)
			{
				Logger.error("Could not load wf props panel:" + wfPanelClass);
				Logger.error(ex);
			}
		}
		mPropertyPanel = new VertexPropertyPanel();
	}

	@Override
	public void control(String control, String msg) {
		// TODO Auto-generated method stub
		
	}

}
