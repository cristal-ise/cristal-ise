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
package org.cristalise.gui.tree;

import java.awt.datatransfer.Transferable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.MainFrame;
import org.cristalise.gui.TreeBrowser;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("serial")
@Slf4j
public class NodeTransferHandler extends TransferHandler {

    TreeBrowser tree;

    public NodeTransferHandler(TreeBrowser treeBrowser) {
        tree = treeBrowser;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    public Transferable createTransferable(JComponent c) {
        Node selNode = tree.getSelectedNode();
        if (selNode instanceof Transferable)
            return (Transferable) selNode;
        else
            return null;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        Node dropNode = tree.getNodeAt(support.getDropLocation().getDropPoint());
        if (dropNode instanceof NodeCollection) {
            NodeCollection collNode = (NodeCollection) dropNode;
            NodeItem source;
            try {
                source = (NodeItem) support.getTransferable().getTransferData(NodeItem.dataFlavor);
                return collNode.addMember(source.getItemPath());
            } catch (Exception e) {
                log.error("", e);
                return false;
            }
        }
        return super.importData(support);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        boolean isNode = support.isDataFlavorSupported(NodeItem.dataFlavor);
        if (!isNode) return false;
        Node dropNode = tree.getNodeAt(support.getDropLocation().getDropPoint());
        if (MainFrame.isAdmin && dropNode instanceof NodeCollection && ((NodeCollection) dropNode).isDependency())
            return true;
        return false;

    }

    @Override
    public Icon getVisualRepresentation(Transferable t) {
        if (t instanceof NodeItem)
            return (((NodeItem) t).getIcon());
        return ImageLoader.nullImg;
    }
}
