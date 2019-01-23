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
package org.cristalise.gui.tabs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Icon;

/**
 * @author Developpement
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
/** * The class which generates the 'X' icon for the tabs. The constructor * accepts an icon which is extra to the 'X' icon, so you can have tabs * like in JBuilder. This value is null if no extra icon is required. */
class CloseTabIcon implements Icon
{
	private int x_pos;
	private int y_pos;
	private int width;
	private int height;
	private Icon fileIcon;
	public CloseTabIcon(Icon fileIcon)
	{
		this.fileIcon = fileIcon;
		width = 16;
		height = 16;
	}
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		this.x_pos = x;
		this.y_pos = y;
		Color col = g.getColor();
		g.setColor(Color.black);
		int y_p = y + 2;
		g.drawLine(x + 1, y_p, x + 12, y_p);
		g.drawLine(x + 1, y_p + 13, x + 12, y_p + 13);
		g.drawLine(x, y_p + 1, x, y_p + 12);
		g.drawLine(x + 13, y_p + 1, x + 13, y_p + 12);
		g.drawLine(x + 3, y_p + 3, x + 10, y_p + 10);
		g.drawLine(x + 3, y_p + 4, x + 9, y_p + 10);
		g.drawLine(x + 4, y_p + 3, x + 10, y_p + 9);
		g.drawLine(x + 10, y_p + 3, x + 3, y_p + 10);
		g.drawLine(x + 10, y_p + 4, x + 4, y_p + 10);
		g.drawLine(x + 9, y_p + 3, x + 3, y_p + 9);
		g.setColor(col);
		if (fileIcon != null)
		{
			fileIcon.paintIcon(c, g, x + width, y_p);
		}
	}
	@Override
	public int getIconWidth()
	{
		return width + (fileIcon != null ? fileIcon.getIconWidth() : 0);
	}
	@Override
	public int getIconHeight()
	{
		return height;
	}
	public Rectangle getBounds()
	{
		return new Rectangle(x_pos, y_pos, width, height);
	}
}