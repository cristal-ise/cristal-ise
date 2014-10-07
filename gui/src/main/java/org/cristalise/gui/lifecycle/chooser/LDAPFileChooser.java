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
import java.awt.Dimension;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.utils.Logger;


//import fr.agilium.gui.tabs.wfPropertiesViewer.JPanelTabbedProperties;
/**
 * @version $Revision: 1.2 $ $Date: 2005/06/27 15:16:12 $
 * @author  $Author: abranson $
 */

public class LDAPFileChooser extends JPanel
{
    public static String SCRIPT_CHOOSER = "Script";
    public static String SCHEMA_CHOOSER = "Schema";
    public static String ACTIVITY_CHOOSER = "Activity";
    private String chooserMode = null;
    public LDAPEntryChooser mLec;
    private boolean mEditable = false;
    DomainPath domainPath;
    String itemQuery = null;
    boolean showversion = true;

    public LDAPFileChooser(String choose)
    {
        super();
        chooserMode = choose;
        initialise();
    }

    private void initialise()
    {
        if (chooserMode.equals(SCHEMA_CHOOSER))
        {
            itemQuery = ClusterStorage.VIEWPOINT + "/Schema/all";
            domainPath = new DomainPath("/desc/OutcomeDesc/");
        }
        else if (chooserMode.equals(SCRIPT_CHOOSER))
        {
            itemQuery = ClusterStorage.VIEWPOINT + "/Script/all";
            domainPath = new DomainPath("/desc/Script/");
        }
        else if (chooserMode.equals(ACTIVITY_CHOOSER))
        {
            domainPath = new DomainPath("/desc/ActivityDesc/");
            showversion = false;
        }
        else
            return;
        mLec = new LDAPEntryChooser(domainPath, mEditable);

        mLec.setPreferredSize(new Dimension(220, 19));
        mLec.setMaximumSize(new Dimension(3000, 22));
        mLec.setMinimumSize(new Dimension(50, 19));
        //mLec.getRenderer().getListCellRendererComponent();

        BoxLayout blyt = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(blyt);
        add(mLec);
        mLec.setVisible(true);
        this.validate();
        this.setVisible(true);

    }

    public String getEntryName()
    {
        return (String) mLec.getSelectedItem();
    }

    public void addItemListener(ItemListener il)
    {
        mLec.addItemListener(il);
    }
    public void setSelectedItem(String name, String version)
    {
        Logger.debug(5,"setSelectedItem " + name + " " + version);
        if (name == null||name.equals("-1")) name="";
        mLec.setSelectedItem(name);
    }

    public void reload()
    {
        mLec.reload();
    }

    public void removeAllItems()
    {
        mLec.removeAllItems();
    }

    /**
     * @param b
     */
    public void setEditable(boolean b)
    {
        mEditable = b;
        mLec.setEditable(b);
    }

    @Override
	public void updateUI()
	{
		if (mLec!=null) mLec.updateUI();
		super.updateUI();
	}

    @Override
	public void setEnabled(boolean enabled)
    {
        mLec.setEnabled(enabled);
    }
}
