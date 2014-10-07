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
import org.cristalise.kernel.lifecycle.AndSplitDef;
import org.cristalise.kernel.lifecycle.JoinDef;
import org.cristalise.kernel.lifecycle.LoopDef;
import org.cristalise.kernel.lifecycle.OrSplitDef;
import org.cristalise.kernel.lifecycle.WfVertexDef;
import org.cristalise.kernel.lifecycle.XOrSplitDef;
import org.cristalise.kernel.utils.Language;



public class SplitJoinDefRenderer implements VertexRenderer
{
    private Paint   mTextPaint                 = Color.black;
    private Paint   mBoxPaint                  = new Color( 204, 204, 204 );
    private Paint   mErrorPaint                = new Color( 255, 50, 0 );
    private boolean mTextOffsetsNotInitialised = true;
    private int     mTextYOffset               = 0;
    private String  mAndText                   = "And";
    private int     mAndTextXOffset            = 0;
    private String  mOrText                    = "Or";
    private int     mOrTextXOffset             = 0;
    private String  mXOrText                   = "XOr";
    private int     mXOrTextXOffset            = 0;
    private String  mJoinText                  = "Join";
    private int     mJoinTextXOffset           = 0;
    private String  mLoopText                   = "Loop";
    private int     mLoopTextXOffset            = 0;
    private String  mRouteText                   = "";
    private int     mRouteTextXOffset            = 0;
    private String  mXXXText                   = "XXX";
    private int     mXXXTextXOffset            = 0;

    @Override
	public void draw( Graphics2D g2d, Vertex vertex)
    {
        GraphPoint centrePoint  = vertex.getCentrePoint();
        String     text         = null;
        int        textXOffset  = 0;
        int        vertexHeight = vertex.getHeight();
        int        vertexWidth  = vertex.getWidth();
        boolean    hasError     = !((WfVertexDef)vertex).verify();


        if ( mTextOffsetsNotInitialised )
        {
            initialiseTextOffsets( g2d );
            mTextOffsetsNotInitialised = false;
        }
        if ( vertex instanceof  LoopDef )
        {
            text        = Language.translate(mLoopText);
            textXOffset = mLoopTextXOffset;
        }
        else if ( vertex instanceof  XOrSplitDef )
        {
            text        = Language.translate(mXOrText);
            textXOffset = mXOrTextXOffset;
        }
        else if ( vertex instanceof OrSplitDef )
        {
            text        = Language.translate(mOrText);
            textXOffset = mOrTextXOffset;
        }
        else if ( vertex instanceof AndSplitDef  )
        {
            text        = Language.translate(mAndText);
            textXOffset = mAndTextXOffset;
        }
        else if ( vertex instanceof JoinDef)
        {
            String type= (String)((JoinDef)vertex).getProperties().get("Type");
            if (type!=null && type.equals("Route"))
            {
                text        = mRouteText;
                textXOffset = mRouteTextXOffset;
            }
            else
            {
                text        = Language.translate(mJoinText);
                textXOffset = mJoinTextXOffset;
            }
        }
        else
        {
            text        = mXXXText;
            textXOffset = mXXXTextXOffset;
        }


        g2d.setPaint( hasError ? mErrorPaint : mBoxPaint );
        g2d.fillRect
        (
            centrePoint.x - vertexWidth / 2,
            centrePoint.y - vertexHeight / 2,
            vertexWidth,
            vertexHeight
        );
        g2d.setPaint( mTextPaint );
        g2d.drawRect
        (
            centrePoint.x - vertexWidth / 2,
            centrePoint.y - vertexHeight / 2,
            vertexWidth,
            vertexHeight
        );
        g2d.drawString( text, centrePoint.x - textXOffset, centrePoint.y + mTextYOffset );
        if (hasError) {
            g2d.setPaint( mErrorPaint );
            String errors = Language.translate(((WfVertexDef)vertex).getErrors());
            int errorWidth = g2d.getFontMetrics().stringWidth( errors );
            g2d.drawString( errors, centrePoint.x - ( errorWidth / 2), centrePoint.y + vertexHeight );
        }
    }

    private void initialiseTextOffsets( Graphics2D g2d )
    {
        FontMetrics metrics = g2d.getFontMetrics();


        mTextYOffset     = metrics.getHeight() / 3;
        mAndTextXOffset  = metrics.stringWidth( Language.translate(mAndText) ) / 2;
        mOrTextXOffset   = metrics.stringWidth( Language.translate(mOrText) ) / 2;
        mXOrTextXOffset  = metrics.stringWidth( Language.translate(mXOrText) ) / 2;
        mJoinTextXOffset = metrics.stringWidth( Language.translate(mJoinText) ) / 2;
        mLoopTextXOffset = metrics.stringWidth( Language.translate(mJoinText) ) / 2;
        mRouteTextXOffset  = metrics.stringWidth( Language.translate(mRouteText) ) / 2;
    }
}

