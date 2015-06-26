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
package org.cristalise.gui.lifecycle.desc;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;

import org.cristalise.gui.graph.view.VertexRenderer;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.ActivitySlotDef;


public class ActivitySlotDefRenderer implements VertexRenderer
{
    private Paint mInactivePaint = new Color(255, 255, 255);
    private Paint mErrorPaint    = new Color( 255, 50, 0 );
	private Paint mCompositePaint= new Color(200, 200, 255);
    private Paint mTextPaint     = Color.black;


    @Override
	public void draw( Graphics2D g2d, Vertex vertex)
    {
        ActivitySlotDef activitySlotDef  = ( ActivitySlotDef )vertex;
        boolean         hasError         = activitySlotDef.verify();
		boolean         isComposite = false;
        isComposite      = activitySlotDef.getIsComposite();
        GraphPoint      centrePoint      = activitySlotDef.getCentrePoint();
        int             vertexHeight     = activitySlotDef.getHeight();
        int             vertexWidth      = activitySlotDef.getWidth();

        String[]        linesOfText      = new String[2+(hasError?0:1)];
        FontMetrics     metrics          = g2d.getFontMetrics();
        int             lineWidth        = 0;
        int             lineHeight       = metrics.getHeight();
        int             linesHeight      = lineHeight * linesOfText.length;
        int             linesStartY      = centrePoint.y - linesHeight / 2 + lineHeight * 2 / 3;
        int             x                = 0;
        int             y                = 0;
        int             i                = 0;

        linesOfText[0]="("+activitySlotDef.getActivityDef()+")";
        linesOfText[1]=(String)activitySlotDef.getProperties().get("Name");

        if (!hasError)linesOfText[2]=activitySlotDef.getErrors();

        g2d.setPaint( !hasError ? mErrorPaint : isComposite ? mCompositePaint : mInactivePaint );
        g2d.fill3DRect
        (
            centrePoint.x - vertexWidth / 2,
            centrePoint.y - vertexHeight / 2,
            vertexWidth,
            vertexHeight,
            true
        );

        g2d.setPaint( mTextPaint );

        // Draw the lines of text
        for ( i = 0; i < linesOfText.length; i++ )
        {
            if (linesOfText[i] == null) linesOfText[i] = "";
            lineWidth = metrics.stringWidth( linesOfText[ i ] );
            x = centrePoint.x - lineWidth / 2;
            y = linesStartY + i * lineHeight;
            g2d.drawString( linesOfText[ i ], x, y );
        }
    }
}

