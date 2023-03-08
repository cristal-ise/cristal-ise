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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.graph.controller.DeletionController;
import org.cristalise.gui.graph.controller.StartVertexController;
import org.cristalise.kernel.graph.model.GraphModelManager;
import org.cristalise.kernel.graph.model.TypeNameAndConstructionInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool bar with mode toggle buttons, start and delete buttons, and the possibility to add other
 * arbitrary buttons at constructions time.
 */
@Slf4j
public class EditorToolBar extends Box implements Printable {
    protected boolean mEdgeCreationMode = false; // True if edges can be created
    protected GraphPanel mGraphPanel = null;

    // There is on mode button listener per mode button.
    // When a mode button fires anaction performed event
    // its corresponding listener notifies all of the
    // editor mode listeners.
    protected class ModeButtonListener implements ActionListener {
        protected String mModeId = null;

        public ModeButtonListener(String modeId) {
            mModeId = modeId;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            notifyListeners(mModeId);
        }
    }

    // Vertex types and ids
    protected JComboBox<TypeNameAndConstructionInfo> mVertexTypeBox =
            new JComboBox<TypeNameAndConstructionInfo>();
    // Edge types and ids
    protected JComboBox<TypeNameAndConstructionInfo> mEdgeTypeBox =
            new JComboBox<TypeNameAndConstructionInfo>();
    // Mode buttons
    protected ButtonGroup mModeButtonGroup = new ButtonGroup();
    protected JToggleButton mVertexModeButton =
            new JToggleButton(ImageLoader.findImage("graph/newvertex.png"));
    protected JToggleButton mSelectModeButton =
            new JToggleButton(ImageLoader.findImage("graph/selection.gif"));
    protected JToggleButton mEdgeModeButton =
            new JToggleButton(ImageLoader.findImage("graph/edge.png"));
    // Normal buttons
    protected JButton[] mOtherButtons = null;
    protected JButton mStartButton = new JButton(ImageLoader.findImage("graph/start.png"));
    protected JButton mDeleteButton = new JButton(ImageLoader.findImage("graph/delete.png"));
    protected JButton mPrintButton = new JButton(ImageLoader.findImage("graph/print.png"));
    protected JButton mCopyButton = new JButton(ImageLoader.findImage("graph/copy.png"));
    // Controllers
    protected StartVertexController mStartVertexController = new StartVertexController();
    protected DeletionController mDeletionController = new DeletionController();
    // Editor mode listeners
    protected Vector<EditorModeListener> mListenerVector = new Vector<EditorModeListener>(10, 10);

    public EditorToolBar(boolean edgeCreationMode, // True if edges can be created
            JButton[] otherButtons, GraphPanel graphP) {
        super(BoxLayout.X_AXIS);
        mGraphPanel = graphP;
        mEdgeCreationMode = edgeCreationMode;
        mOtherButtons = otherButtons;
        prepareModeButtons();
        mStartVertexController.setStartButton(mStartButton);
        mDeletionController.setDeleteButton(mDeleteButton);
        createLayout();
        createListeners();
    }

    protected void prepareModeButtons() {
        // Set the tool tip texts
        mVertexModeButton.setToolTipText("Create vertex");
        mSelectModeButton.setToolTipText("Multi-select and drag");
        mEdgeModeButton.setToolTipText("Create edge");
        mStartButton.setToolTipText("Select the start vertex of the graph");
        mDeleteButton.setToolTipText("Delete the selection");
        mPrintButton.setToolTipText("Print this graph");
        mCopyButton.setToolTipText("Copy an image of this graph to the clipboard");
        // Set the button margins to 0
        mVertexModeButton.setMargin(new Insets(0, 0, 0, 0));
        mSelectModeButton.setMargin(new Insets(0, 0, 0, 0));
        mEdgeModeButton.setMargin(new Insets(0, 0, 0, 0));
        // The initial mode is select mode
        mSelectModeButton.setSelected(true);
        // Add the mode buttons to the mode button group
        mModeButtonGroup.add(mVertexModeButton);
        mModeButtonGroup.add(mSelectModeButton);
        mModeButtonGroup.add(mEdgeModeButton);
        // Add the action listeners
        mVertexModeButton.addActionListener(new ModeButtonListener("Vertex"));
        mSelectModeButton.addActionListener(new ModeButtonListener("Select"));
        mEdgeModeButton.addActionListener(new ModeButtonListener("Edge"));
    }

    public void enterSelectMode() {
        mSelectModeButton.setSelected(true);
        notifyListeners("Select");
    }

    public void updateVertexTypes(TypeNameAndConstructionInfo[] typeNameAndConstructionInfo) {
        int i = 0;
        mVertexTypeBox.removeAllItems();
        for (i = 0; i < typeNameAndConstructionInfo.length; i++) {
            mVertexTypeBox.addItem(typeNameAndConstructionInfo[i]);
        }
    }

    public void updateEdgeTypes(TypeNameAndConstructionInfo[] typeNameAndConstructionInfo) {
        int i = 0;
        mEdgeTypeBox.removeAllItems();
        for (i = 0; i < typeNameAndConstructionInfo.length; i++) {
            mEdgeTypeBox.addItem(typeNameAndConstructionInfo[i]);
        }
    }

    public TypeNameAndConstructionInfo getSelectedVertexType() {
        return (TypeNameAndConstructionInfo) mVertexTypeBox.getSelectedItem();
    }

    public TypeNameAndConstructionInfo getSelectedEdgeType() {
        return (TypeNameAndConstructionInfo) mEdgeTypeBox.getSelectedItem();
    }

    protected void createLayout() {
        int i = 0;
        add(mSelectModeButton);
        add(mVertexModeButton);
        add(mVertexTypeBox);
        add(Box.createHorizontalStrut(10));
        if (mEdgeCreationMode) {
            add(mEdgeModeButton);
            add(mEdgeTypeBox);
        }
        add(Box.createGlue());
        mPrintButton.setEnabled(true);
        mPrintButton.setMargin(new Insets(0, 0, 0, 0));
        add(mPrintButton);
        mCopyButton.setEnabled(true);
        mCopyButton.setMargin(new Insets(0, 0, 0, 0));
        add(mCopyButton);
        mStartButton.setEnabled(false);
        mStartButton.setMargin(new Insets(0, 0, 0, 0));
        mDeleteButton.setEnabled(false);
        mDeleteButton.setMargin(new Insets(0, 0, 0, 0));
        add(mDeleteButton);
        add(Box.createRigidArea(new Dimension(20, 0)));
        add(mStartButton);
        if (mOtherButtons != null) {
            for (i = 0; i < mOtherButtons.length; i++) {
                mOtherButtons[i].setMargin(new Insets(0, 0, 0, 0));
                add(mOtherButtons[i]);
            }
        }
    }

    protected void createListeners() {
        // The vertex mode button should be selected if the
        // user select a vertex type from the vertex type box
        mVertexTypeBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                mVertexModeButton.setSelected(true);
                notifyListeners("Vertex");
            }
        });
        // The edge mode button should be selected if the
        // user select an edge type from the edge type box
        mEdgeTypeBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                mEdgeModeButton.setSelected(true);
                notifyListeners("Edge");
            }
        });
        mPrintButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                PrinterJob _monJob = PrinterJob.getPrinterJob();
                if (_monJob.printDialog())
                    _monJob.setPrintable(self());
                try {
                    _monJob.print();
                } catch (Exception ex) {
                }
            }
        });

        try {
            Class.forName("java.awt.datatransfer.DataFlavor").getDeclaredField("imageFlavor");
            mCopyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    try {
                        Image i = createImage(mGraphPanel.getWidth(), mGraphPanel.getHeight());
                        Graphics g = i.getGraphics();
                        mGraphPanel.paintComponent(g);
                        ImageTransferable it = new ImageTransferable(i, mGraphPanel.getWidth(),
                                mGraphPanel.getHeight());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(it, it);
                    } catch (Exception e) {
                        log.error("",e);
                    }
                }
            });
        } catch (Exception ex) { // image clipboard transfer not supported
            mCopyButton.setEnabled(false);
        }
    }

    protected class ImageTransferable implements Transferable, ClipboardOwner {
        Image image;
        int width, height;
        DataFlavor javaImg;

        public ImageTransferable(Image image, int width, int height) {
            this.image = image;
            this.width = width;
            this.height = height;
            try {
                javaImg = new DataFlavor("image/x-java-image; class=java.awt.Image", "AWT Image");
            } catch (Exception ex) {
            }
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor) || image == null) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            boolean result = in(flavor, getTransferDataFlavors());
            return result;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {javaImg};
        }

        protected boolean in(DataFlavor flavor, DataFlavor[] flavors) {
            int f = 0;
            while ((f < flavors.length) && !flavor.equals(flavors[f])) {
                f++;
            }
            return f < flavors.length;
        }

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            image = null;
        }
    }

    protected void notifyListeners(String newModeId) {
        int i = 0;
        EditorModeListener listener = null;
        for (i = 0; i < mListenerVector.size(); i++) {
            listener = mListenerVector.elementAt(i);
            listener.editorModeChanged(newModeId);
        }
    }

    public void setGraphModelManager(GraphModelManager graphModelManager) {
        mStartVertexController.setGraphModelManager(graphModelManager);
        mDeletionController.setGraphModelManager(graphModelManager);
    }

    public void setGraphPanel(GraphPanel graphPanel) {
        graphPanel.addKeyListener(mDeletionController);
        mDeletionController.setGraphPanel(graphPanel);
    }

    public void addEditorModeListener(EditorModeListener listener) {
        mListenerVector.add(listener);
    }

    public void removeEditorModeListener(EditorModeListener listener) {
        mListenerVector.remove(listener);
    }

    public void setGraphEditable(boolean editable) {
        mVertexModeButton.setEnabled(editable);
        mEdgeModeButton.setEnabled(editable);
    }

    public EditorToolBar self() {
        return this;
    }

    @Override
    public int print(Graphics g, PageFormat pf, int i) throws PrinterException {
        if (i >= 1) {
            return Printable.NO_SUCH_PAGE;
        }
        Graphics2D g2d = (Graphics2D) g;
        double scalex = pf.getImageableWidth() / mGraphPanel.getWidth();
        double scaley = pf.getImageableHeight() / mGraphPanel.getHeight();
        double scale = Math.min(Math.min(scalex, scaley), 1);
        g2d.translate(pf.getImageableX(), pf.getImageableY());
        g2d.scale(scale, scale);
        mGraphPanel.printComponent(g2d);
        return Printable.PAGE_EXISTS;
    }
}
