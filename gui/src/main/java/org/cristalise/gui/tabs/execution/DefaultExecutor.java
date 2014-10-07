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
package org.cristalise.gui.tabs.execution;

import javax.swing.JLabel;

import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.utils.Language;


/**************************************************************************
 *
 * $Revision: 1.2 $
 * $Date: 2003/11/04 14:31:30 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class DefaultExecutor implements Executor {

    public DefaultExecutor() {
        super();
    }

    @Override
	public void execute(Job job, JLabel status) throws Exception {
        status.setText(Language.translate("Submitting..."));
        MainFrame.progress.startBouncing("Requesting, please wait.");
        MainFrame.userAgent.execute(job);
        MainFrame.progress.stopBouncing("Execution complete.");
        status.setText("Waiting for joblist update.");
    }

    @Override
	public String toString() {
        return "Normal";
    }
}
