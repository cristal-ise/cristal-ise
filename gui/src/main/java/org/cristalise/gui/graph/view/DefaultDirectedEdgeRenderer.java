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

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;

import org.cristalise.kernel.graph.model.DirectedEdge;
import org.cristalise.kernel.graph.model.GraphPoint;



public class DefaultDirectedEdgeRenderer implements DirectedEdgeRenderer
{
    private GeneralPath mArrowTemplate = new GeneralPath();
    private Paint       mLinePaint     = null;


    public DefaultDirectedEdgeRenderer(Paint linePaint)
    {
        mLinePaint = linePaint;

        mArrowTemplate.moveTo(-5, 5);
        mArrowTemplate.lineTo( 0, 0);
        mArrowTemplate.lineTo( 5, 5);
    }


    @Override
	public void draw(Graphics2D g2d, DirectedEdge directedEdge)
    {
        GraphPoint      originPoint   = directedEdge.getOriginPoint();
        GraphPoint      terminusPoint = directedEdge.getTerminusPoint();
        GraphPoint      midPoint      = new GraphPoint();
        AffineTransform transform     = new AffineTransform();
        Shape           arrow         = null;

        g2d.setPaint(mLinePaint);
        g2d.drawLine(originPoint.x, originPoint.y, terminusPoint.x, terminusPoint.y);

        midPoint.x = originPoint.x + (terminusPoint.x - originPoint.x)/2;
        midPoint.y = originPoint.y + (terminusPoint.y - originPoint.y)/2;

        transform.translate(midPoint.x, midPoint.y);
        transform.rotate(calcArrowAngle(originPoint.x, originPoint.y, terminusPoint.x, terminusPoint.y));

        arrow = mArrowTemplate.createTransformedShape(transform);

        g2d.draw(arrow);
    }


    private static double calcArrowAngle(int originX, int originY, int terminusX, int terminusY) {
        double width  = terminusX - originX;
        double height = terminusY - originY;

        if((width == 0) && (height > 0))  return Math.PI;

        if((width == 0) && (height < 0))  return 0;

        if((width > 0)  && (height == 0)) return Math.PI/2.0;

        if((width < 0)  && (height == 0)) return -1.0 * Math.PI/2.0;

        if((width > 0)  && (height > 0))  return Math.PI/2.0 + Math.atan(Math.abs(height)/Math.abs(width));

        if((width > 0)  && (height < 0))  return Math.atan(Math.abs(width)/Math.abs(height));

        if((width < 0)  && (height < 0))  return -1.0 * Math.atan(Math.abs(width)/Math.abs(height));

        if((width < 0)  && (height > 0))  return -1.0 * (Math.PI/2.0 + Math.atan(Math.abs(height)/Math.abs(width)));

        return 0.0;
    }
}
