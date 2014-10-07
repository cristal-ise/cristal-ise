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
package org.cristalise.gui.lifecycle.chooser;
/**
 * @version $Revision: 1.2 $ $Date: 2005/12/01 14:23:15 $
 * @author  $Author: abranson $
 */

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.JComboBox;

import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.process.Gateway;


public class LDAPEntryChooser extends JComboBox
{

    DomainPath mDomainPath = null;
    ArrayList<String> allItems = new ArrayList<String>();

    public LDAPEntryChooser(DomainPath domPath, boolean editable)
    {
        super();
        setEditable(editable);
        mDomainPath = domPath;
        initialise();
     }

    private void initialise()
    {
        try
        {
            Iterator<?> children = Gateway.getLookup().search(mDomainPath, "*");
            while (children.hasNext())
            {
                DomainPath domPath = (DomainPath)children.next();
                allItems.add(domPath.getName());
            }
        }
        catch (Exception ex)
        {
        	MainFrame.exceptionDialog(ex);
        }

        Collections.sort(allItems);
        addItem("");
        for (String element : allItems) {
			addItem(element);
		}

    }

    public void reload()
    {
        removeAllItems();
        initialise();
    }

    @Override
	public synchronized Dimension getSize()
    {
        if (Gateway.getProperties().getInt("ResizeCombo") > 0)
            return new Dimension(super.getSize().width<400?400:super.getSize().width,super.getSize().height);
        return super.getSize();
    }

}
