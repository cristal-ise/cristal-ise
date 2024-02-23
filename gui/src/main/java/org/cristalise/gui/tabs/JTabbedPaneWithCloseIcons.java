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
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cristalise.gui.ItemDetails;


/**
 * @author Developpement
 *
 * Allows a close icone in JTabbePane
 */
@SuppressWarnings("serial")
public class JTabbedPaneWithCloseIcons extends JTabbedPane implements MouseListener, ChangeListener
{
	/**
	 *
	 */
	public JTabbedPaneWithCloseIcons()
	{
		super();
		addMouseListener(this);
		addChangeListener(this);
	}
	/**
	 * @see javax.swing.JTabbedPane#addTab(String, Icon, Component, String)
	 */
	@Override
	public void addTab(String title, Icon arg2, Component component, String arg3)
	{
		super.addTab(title, new CloseTabIcon(arg2), component, arg3);
	}
	/**
	 * @see java.awt.event.MouseListener#mouseClicked(MouseEvent)
	 */
	@Override
	public void mouseClicked(MouseEvent e)
	{
        int tabNumber = getUI().tabForCoordinate(this, e.getX(), e.getY());
		Component cp = null;
		if (tabNumber < 0)
			return;
		Rectangle rect = ((CloseTabIcon) getIconAt(tabNumber)).getBounds();
        if (rect.contains(e.getX(), e.getY())||(e.getModifiers()& InputEvent.CTRL_MASK) != 0)
		{ //the tab is being closed
			cp = this.getComponent(tabNumber);
			//if (getComponentCount() != 1)
				if (cp instanceof ItemDetails)
				{
					((ItemDetails) cp).closeTab();
                    remove(cp);
				}
		}
		stateChanged(new ChangeEvent(this));
	}
	/**
	 * @see java.awt.event.MouseListener#mouseEntered(MouseEvent)
	 */
	@Override
	public void mouseEntered(MouseEvent e)
	{
	}
	/**
	 * @see java.awt.event.MouseListener#mouseExited(MouseEvent)
	 */
	@Override
	public void mouseExited(MouseEvent e)
	{
	}
	/**
	 * @see java.awt.event.MouseListener#mousePressed(MouseEvent)
	 */
	@Override
	public void mousePressed(MouseEvent e)
	{
	}
	/**
	 * @see java.awt.event.MouseListener#mouseReleased(MouseEvent)
	 */
	@Override
	public void mouseReleased(MouseEvent e)
	{
	}
	@Override
	public void stateChanged(ChangeEvent e)
	{
		if (getSelectedComponent()!= null)
        ((ItemDetails) getSelectedComponent()).refresh();
	}
}
