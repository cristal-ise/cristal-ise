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
package org.cristalise.gui.graph.controller;

import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.Vertex;



public class Selection
{
    // Either a single edge can be selected or
    // one or more vertices can be selected.
    // It is impossible to select an edge and a
    // vertex at the same time.
    public DirectedEdge mEdge         = null;
    public Vertex[]     mVertices     = null;
    public int          mTopLeftX     = 0;
    public int          mTopLeftY     = 0;
    public int          mBottomRightX = 0;
    public int          mBottomRightY = 0;


    public Selection(DirectedEdge edge,
                     Vertex[]     vertices,
                     int          topLeftX,
                     int          topLeftY,
                     int          bottomRightX,
                     int          bottomRightY)
    {
        mEdge         = edge;
        mVertices     = vertices;
        mTopLeftX     = topLeftX;
        mTopLeftY     = topLeftY;
        mBottomRightX = bottomRightX;
        mBottomRightY = bottomRightY;
    }
}
