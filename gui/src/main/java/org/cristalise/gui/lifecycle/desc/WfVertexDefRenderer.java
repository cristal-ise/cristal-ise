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
package org.cristalise.gui.lifecycle.desc;

import java.awt.Graphics2D;

import org.cristalise.gui.graph.view.VertexRenderer;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.ActivityDef;
import org.cristalise.kernel.lifecycle.ActivitySlotDef;
import org.cristalise.kernel.lifecycle.AndSplitDef;
import org.cristalise.kernel.lifecycle.JoinDef;


public class WfVertexDefRenderer implements VertexRenderer
{
    protected ActivityDefRenderer mActivityDefRenderer = new ActivityDefRenderer();
    protected SplitJoinDefRenderer    mSplitJoinDefRenderer    = new SplitJoinDefRenderer();


    @Override
	public void draw( Graphics2D g2d, Vertex vertex)
    {
        if ( vertex instanceof ActivitySlotDef || vertex instanceof ActivityDef)
        {
            mActivityDefRenderer.draw( g2d, vertex);
        }
        else if ( ( vertex instanceof AndSplitDef ) || ( vertex instanceof JoinDef ) )
        {
            mSplitJoinDefRenderer.draw( g2d, vertex);
        }
    }
}

