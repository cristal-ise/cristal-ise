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
package org.cristalise.gui.collection;

import org.cristalise.gui.graph.view.VertexPropertyPanel;
import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.AggregationMember;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.graph.model.Vertex;


public class PropertyPanel extends VertexPropertyPanel {

    Aggregation mCollection;

    public PropertyPanel() {
       super(false);
    }

    public void setCollection(Aggregation collection) {
        mCollection = collection;
    }

    @Override
	public void setVertex(Vertex vert) {
        try {
            CollectionMember newMember = mCollection.getMember(vert.getID());
            if (newMember instanceof AggregationMember) {
                super.setVertex((AggregationMember)newMember);
                return;
            }
            else
                clear();
        } catch (ObjectNotFoundException ex) {
            clear();
            selObjClass.setText("No Collection Member object found");
        }
    }
}
