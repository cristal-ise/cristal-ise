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


import java.util.ArrayList;

import org.cristalise.gui.ItemTabManager;
import org.cristalise.kernel.lookup.Path;


/**
 * Structure for Item presence on the tree and ItemDetails boxes. Created by NodeFactory.
 * @author $Author: abranson $
 * @version $Version$
 */
public class NodeAgent extends NodeItem {

    public NodeAgent(Path path, ItemTabManager desktop) {
        super(path, desktop);
    }

    @Override
	public ArrayList<String> getTabs() {

        ArrayList<String> requiredTabs = super.getTabs();
        requiredTabs.add("JobList");
        return requiredTabs;
    }
}
