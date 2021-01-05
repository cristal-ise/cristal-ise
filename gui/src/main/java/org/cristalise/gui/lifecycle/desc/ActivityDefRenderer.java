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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.ArrayList;

import org.cristalise.gui.graph.view.VertexRenderer;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.ActivitySlotDef;
import org.cristalise.kernel.lifecycle.WfVertexDef;


public class ActivityDefRenderer implements VertexRenderer
{
    private Paint mInactivePaint = new Color(255, 255, 255);
    private Paint mErrorPaint    = new Color( 255, 50, 0 );
	private Paint mCompositePaint= new Color(200, 200, 255);
    private Paint mTextPaint     = Color.black;


    @Override
	public void draw( Graphics2D g2d, Vertex vertex)
    {
        WfVertexDef      activityDef      = ( WfVertexDef )vertex;
        boolean          hasError         = activityDef.verify();
        GraphPoint       centrePoint      = activityDef.getCentrePoint();
        int              vertexHeight     = activityDef.getHeight();
        int              vertexWidth      = activityDef.getWidth();

        ArrayList<String>linesOfText      = new ArrayList<String>();
        FontMetrics      metrics          = g2d.getFontMetrics();
        int              lineHeight       = metrics.getHeight();

        int              x                = 0;
        int              y                = 0;
        int              i                = 0;

        if (activityDef instanceof ActivitySlotDef) {
        	try {
        		linesOfText.add((String)activityDef.getProperties().get("Name"));
        		linesOfText.add("("+((ActivitySlotDef)activityDef).getTheActivityDef(null).getActName()+")");
        	} catch (Exception e) {
        		linesOfText.add("(Not found)");
        	}
        }
        else
        	linesOfText.add(activityDef.getName());
        

        if (!hasError)
        	linesOfText.add(activityDef.getErrors());
        

        g2d.setPaint( !hasError ? mErrorPaint : activityDef.getIsComposite() ? mCompositePaint : mInactivePaint );
        g2d.fill3DRect
        (
            centrePoint.x - vertexWidth / 2,
            centrePoint.y - vertexHeight / 2,
            vertexWidth,
            vertexHeight,
            true
        );

        g2d.setPaint( mTextPaint );

        int linesHeight = lineHeight * linesOfText.size();
        int linesStartY = centrePoint.y - linesHeight / 2 + lineHeight * 2 / 3;
        // Draw the lines of text

        for (String line : linesOfText) {
            if (line == null) line = "";
            int lineWidth = metrics.stringWidth( line );
            x = centrePoint.x - lineWidth / 2;
            y = linesStartY + i++ * lineHeight;
            g2d.drawString( line, x, y );
        }
    }
}

