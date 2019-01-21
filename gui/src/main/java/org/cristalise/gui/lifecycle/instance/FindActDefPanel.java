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
package org.cristalise.gui.lifecycle.instance;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JButton;

import org.cristalise.gui.MainFrame;
import org.cristalise.gui.graph.view.SelectedVertexPanel;
import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.ActivitySlotDef;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;


/**************************************************************************
 *
 * $Revision: 1.3 $
 * $Date: 2005/12/01 14:23:15 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class FindActDefPanel extends SelectedVertexPanel {

    JButton findButton;
    ActivitySlotDef currentAct;

    public FindActDefPanel() {
        super();
        findButton = new JButton("Open Definition");
        findButton.setEnabled(false);
        add(findButton);
        findButton.addActionListener(new ActionListener()
                {
            @Override
			public void actionPerformed(ActionEvent e)
            {
				Iterator<Path> acts = Gateway.getLookup().search(new DomainPath("/desc/ActivityDesc/"), currentAct.getActivityDef());
                if (acts.hasNext()) MainFrame.itemFinder.getDefaultConsumer().push((DomainPath)acts.next());
            }
        });
    }

    /**
     *
     */

    @Override
	public void select(Vertex vert) {
        if (vert instanceof ActivitySlotDef) {
            findButton.setEnabled(true);
            currentAct = (ActivitySlotDef)vert;
        }
        else
            clear();

    }

    /**
     *
     */

    @Override
	public void clear() {
        findButton.setEnabled(false);
        currentAct = null;
    }

}
