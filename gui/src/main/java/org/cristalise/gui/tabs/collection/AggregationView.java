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
package org.cristalise.gui.tabs.collection;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JSplitPane;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.collection.AggregationMemberRenderer;
import org.cristalise.gui.collection.PropertyPanel;
import org.cristalise.gui.collection.SelectedMemberPanel;
import org.cristalise.gui.graph.view.EditorPanel;
import org.cristalise.gui.graph.view.GraphPanel;
import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.AggregationMember;
import org.cristalise.kernel.collection.AggregationVertexFactory;
import org.cristalise.kernel.collection.AggregationVertexOutlineCreator;
import org.cristalise.kernel.collection.Collection;

/**
 * @version $Revision: 1.5 $ $Date: 2006/09/15 15:02:24 $
 * @author $Author: abranson $
 */
public class AggregationView extends CollectionView<AggregationMember>
{
	protected JButton mSaveButton = new JButton(ImageLoader.findImage("graph/save.png"));
	protected JButton mHistoryButton = new JButton(ImageLoader.findImage("graph/history.png"));
	protected JButton[] mOtherToolBarButtons = { mSaveButton, mHistoryButton };
	// Graph editor panel
	protected EditorPanel mEditorPanel;
	// Objects to view/modify the properties of the selected activity
	protected PropertyPanel mPropertyPanel;
	protected JSplitPane mSplitPane;
	private final AggregationVertexFactory mAggregationVertexFactory = new AggregationVertexFactory();
	private final AggregationMemberRenderer mAggregationMemberRenderer = new AggregationMemberRenderer();
	public AggregationView()
	{
		super();
        setLayout(new GridLayout(1,1));
		mPropertyPanel = new PropertyPanel();
		mEditorPanel = new EditorPanel(null, mAggregationVertexFactory, new AggregationVertexOutlineCreator(), false, mOtherToolBarButtons, new GraphPanel(null, mAggregationMemberRenderer));
		createLayout();
		createListeners();
		mPropertyPanel.setGraphModelManager(mEditorPanel.mGraphModelManager);
        mPropertyPanel.createLayout(new SelectedMemberPanel());
        mEditorPanel.setEditable(MainFrame.isAdmin);
	}

	@Override
	public void setCollection(Collection<AggregationMember> contents)
	{
        thisColl = contents;
        Aggregation agg = (Aggregation)thisColl;
        mPropertyPanel.setCollection(agg);
        mAggregationMemberRenderer.setAggregation(agg);
		mEditorPanel.mGraphModelManager.setModel(agg.getLayout());
		mEditorPanel.updateVertexTypes(agg.getVertexTypeNameAndConstructionInfo());
		mEditorPanel.enterSelectMode();
		mAggregationVertexFactory.setCreationContext(agg);
	}
	public void createLayout()
	{
		mSaveButton.setToolTipText("Save Layout Changes");
		mSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mEditorPanel, mPropertyPanel);
        mSplitPane.setDividerSize(5);
		add(mSplitPane);
	}

	protected void createListeners()
	{
		mSaveButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				saveCollection();
			}
		});
		mHistoryButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				new CollectionHistoryWindow(item, (Aggregation)thisColl);
			}
		});
	}
}
