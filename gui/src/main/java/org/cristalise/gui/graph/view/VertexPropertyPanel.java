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
package org.cristalise.gui.graph.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.Observable;
import java.util.Observer;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.graph.event.SelectionChangedEvent;
import org.cristalise.gui.tabs.ItemTabPane;
import org.cristalise.kernel.graph.event.EntireModelChangedEvent;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.GraphableEdge;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.utils.CastorHashMap;


public class VertexPropertyPanel extends JPanel implements Observer, TableModelListener, ActionListener {

    private final PropertyTableModel mPropertyModel;
    private final PropertyTable mPropertyTable;
    private GraphModelManager mGraphModelManager;
    private boolean isEditable = false;
    GridBagLayout gridbag = new GridBagLayout();
    protected JLabel selObjName;
    protected JLabel selObjClass;
    JButton addPropButton;
    JButton delPropButton;
    Box newPropBox;
    private JTextField newPropName;
    private JComboBox newPropType;
    private JCheckBox newPropAbstract;
    private boolean useAbstract;
    String[] typeOptions = { "String", "Boolean", "Integer", "Float" };
    String[] typeInitVal = { "", "false", "0", "0.0"};
    SelectedVertexPanel mSelPanel;

    public VertexPropertyPanel(boolean useAbstract) {
        super();
        setLayout(gridbag);
        this.useAbstract = useAbstract;
        mPropertyModel = new PropertyTableModel(useAbstract);
        mPropertyModel.addTableModelListener(this);
        mPropertyTable = new PropertyTable(mPropertyModel);
    }

    /**
    *
    */

    @Override
	public void update(Observable o, Object arg) {
        Vertex[] selectedVertices = null;
        DirectedEdge selectedEdge = null;
        // If the selection has changed
        if (arg instanceof SelectionChangedEvent)
        {
            SelectionChangedEvent event = (SelectionChangedEvent) arg;
            selectedVertices = event.mSelection.mVertices;
            if (selectedVertices != null)
            {
                if (selectedVertices.length == 1)
                {
                    setVertex(selectedVertices[0]);
                    return;
                }
            }
            selectedEdge = event.mSelection.mEdge;
            if (selectedEdge != null)
            {
                setEdge(selectedEdge);
                return;
            }
        }
        if (arg instanceof SelectionChangedEvent || arg instanceof EntireModelChangedEvent){
            clear();
        }
    }


    @Override
	public void tableChanged(TableModelEvent e) {
        if (mGraphModelManager!=null)
            mGraphModelManager.forceNotify();

    }

    public void setVertex(Vertex vert) {
        String vertName = vert.getName();

        if ("domain".equals(vertName)) {
            selObjName.setText("Domain Workflow");
        }
        else {
            if (StringUtils.isNotBlank(vertName)) selObjName.setText(vertName+" - ID:"+vert.getID());
            else                                  selObjName.setText("ID:"+vert.getID());
        }

        selObjClass.setText(vert.getClass().getSimpleName());

        if (mSelPanel != null) mSelPanel.select(vert);
        if (vert instanceof GraphableVertex) {
            mPropertyModel.setMap(((GraphableVertex)vert).getProperties());
            addPropButton.setEnabled(isEditable);
            delPropButton.setEnabled(isEditable);
        }
    }

    public void setEdge(DirectedEdge edge) {
        String edgeName = edge.getName();

        if (StringUtils.isNotBlank(edgeName)) selObjName.setText(edgeName+" - ID:"+edge.getID());
        else                                  selObjName.setText("ID:"+edge.getID());

        selObjClass.setText(edge.getClass().getSimpleName());

        if (edge instanceof GraphableEdge) {
            mPropertyModel.setMap(((GraphableEdge)edge).getProperties());
            addPropButton.setEnabled(isEditable);
            delPropButton.setEnabled(isEditable);
        }
        if (mSelPanel != null) mSelPanel.clear();
    }

    public void clear() {
        selObjName.setText("");
        selObjClass.setText("Nothing Selected");
        mPropertyModel.setMap(new CastorHashMap());
        if (mSelPanel != null) mSelPanel.clear();
        addPropButton.setEnabled(false);
        delPropButton.setEnabled(false);
    }

    /**
     * @param isEditable The isEditable to set.
     */
    public void setEditable(boolean editable) {
        mPropertyModel.setEditable(editable);
        isEditable = editable;
        newPropBox.setVisible(editable);
    }

    public void setGraphModelManager(GraphModelManager manager) {
        mGraphModelManager = manager;
        manager.addObserver(this);
    }

    public void createLayout(SelectedVertexPanel selPanel)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.ipadx = 5;
        c.ipady = 5;

        selObjName = new JLabel();
        selObjName.setFont(ItemTabPane.titleFont);
        gridbag.setConstraints(selObjName, c);
        add(selObjName);

        c.gridy++;
        selObjClass = new JLabel();
        gridbag.setConstraints(selObjClass, c);
        add(selObjClass);

        c.gridy++;
        JLabel title = new JLabel("Properties");
        title.setFont(ItemTabPane.titleFont);
        gridbag.setConstraints(title, c);
        add(title);

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 2;
        JScrollPane scroll = new JScrollPane(mPropertyTable);
        gridbag.setConstraints(scroll, c);
        add(scroll);

        newPropBox = Box.createHorizontalBox();
        newPropBox.add(new JLabel("New :"));
        newPropBox.add(Box.createHorizontalGlue());
        newPropName = new JTextField(15);
        newPropBox.add(newPropName);
        newPropType = new JComboBox(typeOptions);
        newPropBox.add(newPropType);
        newPropBox.add(Box.createHorizontalGlue());
        newPropAbstract = new JCheckBox();
    	if (useAbstract) {
                newPropBox.add(newPropAbstract);
                newPropBox.add(Box.createHorizontalStrut(1));
                newPropBox.add(new JLabel("Abstract"));
                newPropBox.add(Box.createHorizontalStrut(1));
    	}
        addPropButton = new JButton("Add");
        addPropButton.setMargin(new Insets(0, 0, 0, 0));
        delPropButton = new JButton("Del");
        delPropButton.setMargin(new Insets(0, 0, 0, 0));
        addPropButton.addActionListener(this);
        delPropButton.addActionListener(this);
        newPropBox.add(addPropButton);
        newPropBox.add(delPropButton);

        c.gridy++;
        c.weighty=0;
        c.fill= GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(newPropBox, c);
        add(newPropBox);

        if (selPanel != null) {
            c.gridy++;
            mSelPanel = selPanel;
            gridbag.setConstraints(mSelPanel, c);
            add(mSelPanel);
        }
    }

    @Override
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addPropButton) {
            if (newPropName.getText().length() < 1) {
                JOptionPane.showMessageDialog(this, "Enter a name for the new property", "Cannot add property", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (mPropertyModel.sourceMap.containsKey(newPropName.getText())) {
                JOptionPane.showMessageDialog(this, "Property '"+newPropName.getText()+"' already exists.", "Cannot add property", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (mPropertyTable.getCellEditor() != null)
            	mPropertyTable.getCellEditor().stopCellEditing();

            try {
                Class<?> newPropClass = Class.forName("java.lang."+typeOptions[newPropType.getSelectedIndex()]);
                Class<?>[] params = {String.class};
                Constructor<?> init = newPropClass.getConstructor(params);
                Object[] initParams = { typeInitVal[newPropType.getSelectedIndex()] };
                mPropertyModel.addProperty(newPropName.getText(), init.newInstance(initParams), newPropAbstract.isSelected());
            } catch (Exception ex) {
            	MainFrame.exceptionDialog(ex);
            }
        }
        else if (e.getSource() == delPropButton) {
            int selrow = mPropertyTable.getSelectedRow();
            if (selrow == -1) {
                JOptionPane.showMessageDialog(this, "Select a property to remove", "Cannot delete property", JOptionPane.ERROR_MESSAGE);
                return;
            }
            mPropertyModel.delProperty(mPropertyModel.sortedNameList.get(selrow));
        }
    }
}
