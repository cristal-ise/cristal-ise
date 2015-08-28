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
package org.cristalise.gui.tree;

import java.util.Iterator;

import org.cristalise.gui.ItemTabManager;
import org.cristalise.kernel.entity.proxy.DomainPathSubscriber;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;



public class NodeContext extends Node implements DomainPathSubscriber {
    Iterator<Path> children;

    public NodeContext(Path path, ItemTabManager desktop) {
        super(path, desktop);
        this.itemPath = null;
        createTreeNode();
        this.makeExpandable();
        this.type = "Cristal Context";
    }


    @Override
	public void loadChildren() {
        if (children == null) {
            if (binding instanceof DomainPath) Gateway.getProxyManager().subscribeTree(this, (DomainPath)binding);
            children = Gateway.getLookup().getChildren(binding);
        }

        int batch = 75;
        while (children.hasNext() && batch > 0) {
            Path newPath = children.next();
            if (newPath == null) break;
            Logger.msg(2, "Subscription.run() - new node: " + newPath );
            add( newNode(newPath));
            batch--;
        }
        end(children.hasNext());
    }

    @Override
	public void pathAdded(DomainPath path) {
        add(newNode(path));
    }

    @Override
	public void refresh() {
        children = null;
        super.refresh();
    }
    @Override
	public void pathRemoved(DomainPath path) {
        remove(path);
    }

}







