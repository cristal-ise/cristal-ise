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
package org.cristalise.kernel.persistency.outcomebuilder;

import java.util.HashMap;

import org.exolab.castor.xml.schema.ElementDecl;


public class DimensionInstance extends DataRecord {

    //probably will be needed to synch edits later
    Dimension parentDimension;
    int tabNumber;
    String tabName = null;

    public DimensionInstance(ElementDecl model, boolean readOnly, boolean deferred, HashMap<String, Class<?>> specialControls) throws OutcomeException {
        super(model, readOnly, deferred, specialControls);
    }

    public void setTabNumber(int tabNumber) {
        this.tabNumber=tabNumber;
    }

    public void setParent(Dimension parent) {
        this.parentDimension = parent;
    }

    @Override
    public String getName() {
        if (tabName != null) return tabName;
        if (myElement != null && myElement.hasAttribute("name")) {
            return myElement.getAttribute("name");
        }
        else
            return String.valueOf(tabNumber);
    }
}
