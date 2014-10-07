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
package org.cristalise.gui.lifecycle.instance;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;

import org.cristalise.gui.graph.view.VertexRenderer;
import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.Language;

public class ActivityRenderer implements VertexRenderer
{
	private Paint mActivePaint = new Color(100, 255, 100);
	private Paint mActiveCompPaint = new Color(100, 255, 255);
	private Paint mInactivePaint = new Color(255, 255, 255);
	private Paint mInactiveCompPaint = new Color(200, 200, 255);
	private Paint mErrorPaint = new Color(255, 50, 0);
	private Paint mTextPaint = Color.black;
	@Override
	public void draw(Graphics2D g2d, Vertex vertex)
	{
		Activity activity = (Activity) vertex;
		boolean active = activity.getActive();
		boolean hasError = !activity.verify();
		boolean isComposite = activity.getIsComposite();
		GraphPoint centrePoint = activity.getCentrePoint();
		//String description = activity.getDescription();
		String[] linesOfText = new String[3];
		linesOfText[0] = "(" + activity.getType() + ")";
		linesOfText[1] = activity.getName();
		if (hasError)
			linesOfText[2] = Language.translate(activity.getErrors());
		else
		{
			boolean showTime = activity.getActive() && ((Boolean) activity.getProperties().get("Show time")).booleanValue();
			String stateName = "Invalid State"; 
			try {
				stateName = activity.getStateName();
			} catch (InvalidDataException ex) { }
			
			linesOfText[2] = 
					Language.translate(stateName) + (showTime ? " " + getWaitTime(activity.getStateDate()) : "");
		}

		FontMetrics metrics = g2d.getFontMetrics();
		int lineWidth = 0;
		int lineHeight = metrics.getHeight();
		int linesHeight = lineHeight * linesOfText.length;
		int linesStartY = centrePoint.y - linesHeight / 2 + lineHeight * 2 / 3;
		int x = 0;
		int y = 0;
		int i = 0;
		GraphPoint[] outline = vertex.getOutlinePoints();
		Paint actColour;
		if (hasError)
			actColour = mErrorPaint;
		else if (active)
			if (isComposite)
				actColour = mActiveCompPaint;
			else
				actColour = mActivePaint;
		else if (isComposite)
			actColour = mInactiveCompPaint;
		else
			actColour = mInactivePaint;
		g2d.setPaint(actColour);
		//g2d.fill3DRect( centrePoint.x - mSize.width / 2, centrePoint.y - mSize.height / 2, mSize.width, mSize.height, true );
		g2d.fill(graphPointsToPolygon(outline));
		g2d.setPaint(mTextPaint);
		for (i = 0; i < linesOfText.length; i++)
		{
			lineWidth = metrics.stringWidth(linesOfText[i]);
			x = centrePoint.x - lineWidth / 2;
			y = linesStartY + i * lineHeight;
			g2d.drawString(linesOfText[i], x, y);
		}
	}
	private static Polygon graphPointsToPolygon(GraphPoint[] points)
	{
		Polygon polygon = new Polygon();
		int i = 0;
		for (i = 0; i < points.length; i++)
		{
			polygon.addPoint(points[i].x, points[i].y);
		}
		return polygon;
	}
	private static String getWaitTime(GTimeStamp date)
	{
		GTimeStamp now = new GTimeStamp();
		DateUtility.setToNow(now);
		long diff = DateUtility.diff(now, date);
		long secondes = diff % 60;
		long minutes = (diff / 60) % 60;
		long hours = (diff / 3600) % 24;
		long days = (diff / 3600 / 24);
		if (days > 0)
			return days + " " + Language.translate("d") + " " + hours + " " + Language.translate("h");
		if (hours > 0)
			return hours + " " + Language.translate("h") + " " + minutes + " " + Language.translate("min");
		if (minutes > 0)
			return minutes + " " + Language.translate("min");
		return secondes + " " + Language.translate("sec");
	}
}
