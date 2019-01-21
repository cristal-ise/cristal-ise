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
/*Created on 21 nov. 2003 */
package org.cristalise.gui.lifecycle.instance;

import java.awt.Graphics2D;

import org.cristalise.gui.graph.view.DirectedEdgeRenderer;
import org.cristalise.gui.graph.view.GraphPanel;
import org.cristalise.gui.graph.view.VertexRenderer;
import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.lifecycle.instance.Next;


/** @author XSeb74*/
public class WfGraphPanel extends GraphPanel
{
	public WfGraphPanel(DirectedEdgeRenderer d,VertexRenderer v)
	{
		super(d,v);
	}
	// Draws the highlight of the specified edge
	@Override
	protected void drawEdgeHighlight(Graphics2D g2d, DirectedEdge edge)
	{
		GraphPoint originPoint   = edge.getOriginPoint();
		GraphPoint terminusPoint = edge.getTerminusPoint();
		GraphPoint midPoint = new GraphPoint();

		if ("Straight".equals(((Next)edge).getProperties().get("Type")) || ((Next)edge).getProperties().get("Type") == null)
		{
			midPoint.x = originPoint.x + (terminusPoint.x - originPoint.x) / 2;
			midPoint.y = originPoint.y + (terminusPoint.y - originPoint.y) / 2;
		}
		else if (("Broken +".equals(((Next)edge).getProperties().get("Type"))))
		{
			midPoint.x = (originPoint.x + terminusPoint.x) / 2;
			midPoint.y = (originPoint.y + terminusPoint.y) / 2;
		}
		else if (("Broken -".equals(((Next)edge).getProperties().get("Type"))))
		{
			boolean arrowOnY = !(originPoint.y - terminusPoint.y < 60 && originPoint.y - terminusPoint.y > -60);
			midPoint.x = arrowOnY ? terminusPoint.x : (originPoint.x + terminusPoint.x) / 2;
			midPoint.y = arrowOnY ? (originPoint.y + terminusPoint.y) / 2 : originPoint.y;
		}
		else if (("Broken |".equals(((Next)edge).getProperties().get("Type"))))
		{
			boolean arrowOnY = !(originPoint.y - terminusPoint.y < 60 && originPoint.y - terminusPoint.y > -60);
			midPoint.x = arrowOnY ? originPoint.x : (originPoint.x + terminusPoint.x) / 2;
			midPoint.y = arrowOnY ? (originPoint.y + terminusPoint.y) / 2 : terminusPoint.y;
		}
		int minX = midPoint.x - 10;
		int minY = midPoint.y - 10;
		int maxX = midPoint.x + 10;
		int maxY = midPoint.y + 10;
		g2d.drawLine(minX, minY, maxX, minY);
		g2d.drawLine(maxX, minY, maxX, maxY);
		g2d.drawLine(maxX, maxY, minX, maxY);
		g2d.drawLine(minX, maxY, minX, minY);
	}
}
