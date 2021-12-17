/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.graph.model;

import static org.cristalise.kernel.graph.model.BuiltInEdgeProperties.TYPE;
import org.cristalise.kernel.graph.renderer.DefaultDirectedEdgeRenderer.EdgeRouting;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 *
 */
@Accessors(prefix = "m") @Getter @Setter
public abstract class GraphableEdge extends DirectedEdge {

    private GraphableVertex mParent;
    private CastorHashMap   mProperties = null;

    public GraphableEdge() {
        super();
        mProperties = new CastorHashMap();
    }

    public GraphableEdge(GraphableVertex pre, GraphableVertex nex) {
        mProperties = new CastorHashMap();
        setParent(pre.getParent());
        pre.getParent().getChildrenGraphModel().addEdgeAndCreateId(this, pre, nex);
    }

    public KeyValuePair[] getKeyValuePairs() {
        return mProperties.getKeyValuePairs();
    }

    public void setKeyValuePairs(KeyValuePair[] pairs) {
        mProperties.setKeyValuePairs(pairs);
    }

    public Object getBuiltInProperty(BuiltInEdgeProperties prop) {
        return mProperties.get(prop.getName());
    }

    public void setBuiltInProperty(BuiltInEdgeProperties prop, Object val) {
        mProperties.put(prop.getName(), val);
    }

    @Override
    public boolean containsPoint(GraphPoint p) {
        GraphPoint originPoint = getOriginPoint();
        GraphPoint terminusPoint = getTerminusPoint();
        GraphPoint midPoint = new GraphPoint();

        EdgeRouting type = EdgeRouting.getValue(getBuiltInProperty(TYPE).toString());

        switch (type) {
            case BROKEN_PLUS:
                midPoint.x = (originPoint.x + terminusPoint.x) / 2;
                midPoint.y = (originPoint.y + terminusPoint.y) / 2;
                break;

            case BROKEN_MINUS:
                boolean arrowOnY = !(originPoint.y - terminusPoint.y < 60 && originPoint.y - terminusPoint.y > -60);
                midPoint.x = arrowOnY ? terminusPoint.x : (originPoint.x + terminusPoint.x) / 2;
                midPoint.y = arrowOnY ? (originPoint.y + terminusPoint.y) / 2 : originPoint.y;
                break;

            case BROKEN_PIPE:
                arrowOnY = !(originPoint.y - terminusPoint.y < 60 && originPoint.y - terminusPoint.y > -60);
                midPoint.x = arrowOnY ? originPoint.x : (originPoint.x + terminusPoint.x) / 2;
                midPoint.y = arrowOnY ? (originPoint.y + terminusPoint.y) / 2 : terminusPoint.y;
                break;

            default:
                midPoint.x = originPoint.x + (terminusPoint.x - originPoint.x) / 2;
                midPoint.y = originPoint.y + (terminusPoint.y - originPoint.y) / 2;
                break;
        }

        int minX = midPoint.x - 10;
        int minY = midPoint.y - 10;
        int maxX = midPoint.x + 10;
        int maxY = midPoint.y + 10;

        return (p.x >= minX) && (p.x <= maxX) && (p.y >= minY) && (p.y <= maxY);
    }
}
