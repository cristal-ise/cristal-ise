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
import java.util.ArrayList;

import org.cristalise.kernel.entity.Job;


public class ActivityItem {
    public String stepPath;
    public int state;
    public String stateName;
    public String name;
    ArrayList<Job> jobs = new ArrayList<Job>();

    public ActivityItem() {
    	stepPath = "";
    	state = -1;
    	name = "--";
    }

    public ActivityItem(Job thisJob) {
        stepPath = thisJob.getStepPath();
        state = thisJob.getTransition().getOriginStateId();
        stateName = thisJob.getTransition().getOriginState().getName();
        name = thisJob.getStepName();
        jobs.add(thisJob);
    }

    public void addJob(Job newJob) {
        jobs.add(newJob);
    }

    public ArrayList<Job> getJobs() {
        return jobs;
    }

    public String getStepPath() {
        return stepPath;
    }

    @Override
	public String toString() {
        return name+(state>-1?" ("+stateName+")":"");
    }

    @Override
	public boolean equals(Object other) {
        if (other instanceof ActivityItem)
            return hashCode() == ((ActivityItem)other).hashCode();
        return false;
    }

	@Override
	public int hashCode() {
		return stepPath.hashCode();
	}

}
