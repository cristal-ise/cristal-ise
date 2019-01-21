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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.graph.view.EditorPanel;
import org.cristalise.gui.graph.view.VertexPropertyPanel;
import org.cristalise.gui.lifecycle.instance.FindActDefPanel;
import org.cristalise.gui.tabs.outcome.InvalidOutcomeException;
import org.cristalise.gui.tabs.outcome.InvalidSchemaException;
import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.gui.tabs.outcome.OutcomeHandler;
import org.cristalise.gui.tabs.outcome.OutcomeNotInitialisedException;
import org.cristalise.kernel.graph.layout.DefaultGraphLayoutGenerator;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.LifecycleVertexOutlineCreator;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * $Revision: 1.14 $
 * $Date: 2005/09/07 13:46:31 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class CompActDefOutcomeHandler
    extends JPanel
    implements OutcomeHandler {

    protected JButton mLoadButton = new JButton(ImageLoader.findImage("graph/load.png"));
    protected JButton mLayoutButton = new JButton(ImageLoader.findImage("graph/autolayout.png"));
	protected JButton mZoomOutButton = new JButton(ImageLoader.findImage("graph/zoomout.png"));
    protected JButton[] mOtherToolBarButtons = { mZoomOutButton, mLayoutButton, mLoadButton };

    protected CompositeActivityDef mCompActDef = null;
    protected WfEdgeDefFactory mWfEdgeDefFactory = new WfEdgeDefFactory();
    protected WfVertexDefFactory mWfVertexDefFactory = new WfVertexDefFactory();

    protected EditorPanel mEditorPanel;
    protected VertexPropertyPanel mPropertyPanel;
    protected JSplitPane mSplitPane;
    boolean unsaved;

    public CompActDefOutcomeHandler() {
        super();
        mPropertyPanel = loadPropertyPanel();
        mPropertyPanel.createLayout(new FindActDefPanel());
        mEditorPanel =
            new EditorPanel(
                mWfEdgeDefFactory,
                mWfVertexDefFactory,
                new LifecycleVertexOutlineCreator(),
                true,
                mOtherToolBarButtons,
        		new WfDefGraphPanel(new WfDirectedEdgeDefRenderer(),
		new WfVertexDefRenderer()));
    }

    protected void createLayout()
    {
        mLoadButton.setToolTipText("Load from local file");
        mLayoutButton.setToolTipText("Auto-Layout");
        mZoomOutButton.setToolTipText("Zoom Out");

        // Add the editor pane
        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 2.0;
        c.weightx = 2.0;
        mSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mEditorPanel, mPropertyPanel);
        mSplitPane.setDividerSize(5);
        if (mCompActDef != null) {
			int minWidth = mCompActDef.getChildrenGraphModel().getWidth()+20;
			int editWidth = (int)mEditorPanel.getPreferredSize().getWidth();
			if (editWidth > minWidth) minWidth = editWidth;
			if (mSplitPane.getDividerLocation() < minWidth) mSplitPane.setDividerLocation(minWidth);
        }
        gridbag.setConstraints(mSplitPane, c);
        add(mSplitPane);
        revalidate();
    }

    protected void createListeners()
    {
        mLoadButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent ae) {
                File selectedFile = null;

                int returnValue = MainFrame.xmlChooser.showOpenDialog(null);

                switch (returnValue)
                {
                    case JFileChooser.APPROVE_OPTION :
                        selectedFile = MainFrame.xmlChooser.getSelectedFile();
                        try {
                            String newWf = FileStringUtility.file2String(selectedFile);
                            setOutcome(newWf);
                            setUpGraphEditor();
                        } catch (Exception e) {
                        	MainFrame.exceptionDialog(e);
                        }
                    case JFileChooser.CANCEL_OPTION :
                    case JFileChooser.ERROR_OPTION :

                    default :
                }
            }
        });

        mLayoutButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent ae) {
                DefaultGraphLayoutGenerator.layoutGraph(mEditorPanel.mGraphModelManager.getModel());
            }
        });
        
		mZoomOutButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				mEditorPanel.mGraphModelManager.zoomOut();
			}
		});
    }

    public void setUpGraphEditor() {
        mEditorPanel.mGraphModelManager.setModel(mCompActDef.getChildrenGraphModel());
        // Give the editor panel the edge and vertex types
        mEditorPanel.updateVertexTypes(mCompActDef.getVertexTypeNameAndConstructionInfo());
        mEditorPanel.updateEdgeTypes(mCompActDef.getEdgeTypeNameAndConstructionInfo());
        mEditorPanel.enterSelectMode();
        mWfVertexDefFactory.setCreationContext(mCompActDef);
    }

    /**
     *
     */
    @Override
	public void setOutcome(String outcome) throws InvalidOutcomeException {
        try {
            CompositeActivityDef newAct = (CompositeActivityDef)Gateway.getMarshaller().unmarshall(outcome);
            if (mCompActDef != null)
                newAct.setName(mCompActDef.getName());
            mCompActDef = newAct;
            if (mSplitPane != null) {
        		int minWidth = mCompActDef.getChildrenGraphModel().getWidth()+20;
        		if (mSplitPane.getDividerLocation() < minWidth) mSplitPane.setDividerLocation(minWidth);
            }
        } catch (Exception ex) {
            Logger.error(ex);
            throw new InvalidOutcomeException(ex.getMessage());
        }
    }
    /**
     *
     */
    @Override
	public void setDescription(String description)
        throws InvalidSchemaException {
        // ignore - always the same
    }
    /**
     *
     */
    @Override
	public void setReadOnly(boolean readOnly) {
        mLayoutButton.setEnabled(!readOnly);
        mLoadButton.setEnabled(!readOnly);
        mEditorPanel.setEditable(!readOnly);
        mPropertyPanel.setEditable(!readOnly);
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
    	if (!mCompActDef.verify())
    		throw new OutcomeException(mCompActDef.getErrors());
        try {
            return Gateway.getMarshaller().marshall(mCompActDef);
        } catch (Exception ex) {
            throw new OutcomeException(ex.getMessage());
        }
    }
    /**
     *
     */
    @Override
	public void run() {
        Thread.currentThread().setName("Composite Act Def Viewer");
        createLayout();
        createListeners();
        mPropertyPanel.setGraphModelManager(mEditorPanel.mGraphModelManager);
        setUpGraphEditor();
        revalidate();
        doLayout();
    }

    public VertexPropertyPanel loadPropertyPanel()
    {
        String wfPanelClass = Gateway.getProperties().getString("WfPropertyPanel");
        if (wfPanelClass != null) {
            try {
                return (VertexPropertyPanel)Gateway.getProperties().getInstance("WfPropertyPanel");
            } catch (Exception ex) {
                Logger.error("Could not load wf props panel:"+wfPanelClass);
                Logger.error(ex);
            }
        }
        return new VertexPropertyPanel(true);
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
        // FileStringUtility.string2File(targetFile, getOutcome());

        // Make sure module structure is present
        File parentDir = targetFile.getParentFile();
        for (BuiltInResources res : BuiltInResources.values()) {
            FileStringUtility.createNewDir(parentDir.getAbsolutePath() + "/" + res.getTypeCode());
        }

        BufferedWriter imports = new BufferedWriter(new FileWriter(new File(parentDir, mCompActDef.getActName() + "Imports.xml")));
        mCompActDef.export(imports, targetFile.getParentFile(), false);
        imports.close();
    }
}
