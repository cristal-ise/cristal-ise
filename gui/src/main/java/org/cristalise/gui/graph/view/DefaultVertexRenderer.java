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
package org.cristalise.gui.graph.view;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;

import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;



public class DefaultVertexRenderer implements VertexRenderer
{
    private Paint mLinePaint = null;
    private Paint mTextPaint = null;
    private Paint mFillPaint = null;


    public DefaultVertexRenderer(Paint linePaint, Paint textPaint, Paint fillPaint)
    {
        mLinePaint = linePaint;
        mTextPaint = textPaint;
        mFillPaint = fillPaint;
    }


    @Override
	public void draw(Graphics2D g2d, Vertex vertex)
    {
        GraphPoint[]    outlinePoints = vertex.getOutlinePoints();
        GraphPoint      centrePoint   = vertex.getCentrePoint();
        Polygon         outline       = new Polygon();

        String          vertexName    = vertex.getName();
        FontMetrics     metrics       = g2d.getFontMetrics();
        int             textWidth     = metrics.stringWidth(vertexName);
        int             textHeight    = metrics.getHeight();
        int             textX         = centrePoint.x - textWidth/2;
        int             textY         = centrePoint.y + textHeight/3;

        int             i             = 0;


        // Construct a shape in the outline of the vertex
        for(i=0; i<outlinePoints.length; i++)
        {
            outline.addPoint(outlinePoints[i].x, outlinePoints[i].y);
        }

        // Fill and then draw the outline
        g2d.setPaint(mFillPaint);
        g2d.fill(outline);
        g2d.setPaint(mLinePaint);
        g2d.draw(outline);

        // Write the name of the vertex in the centre of the outline
        g2d.setPaint(mTextPaint);
        g2d.drawString(vertexName, textX, textY);
    }
}
