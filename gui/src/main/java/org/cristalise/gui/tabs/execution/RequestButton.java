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
package org.cristalise.gui.tabs.execution;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.utils.Logger;

/**
     * Each job gets a RequestButton
     */

    public class RequestButton extends JButton implements ActionListener {

        Job myJob;
        ActivityViewer parent;

        public RequestButton(Job myJob, ActivityViewer parent) {
            super();
            this.myJob = myJob;
            this.parent = parent;
            String label = myJob.getTransition().getName();
            if (myJob.hasOutcome()) {
            	setBackground(Color.white);
                try {
                	if (myJob.getSchema().getName().equals("Errors")) setBackground(Color.pink);
                } catch (Exception e) {
                	Logger.error(e);
                	MainFrame.exceptionDialog(e);
                	setEnabled(false);
                }
            }
            super.setText(label);
            addActionListener(this);
        }

        @Override
		public void actionPerformed(ActionEvent event) {
            parent.execute(myJob);
        }
    }
