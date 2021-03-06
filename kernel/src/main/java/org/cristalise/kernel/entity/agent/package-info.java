/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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

/**
 * The implementation of Agents, and their Jobs.
 * 
 * <p>The Agent is an extension of the Item that can execute Jobs, and in doing so
 * change the state of Item workflows, submit data to them in the form of Outcomes
 * and run any scripts associated with those activities. In this server object,
 * none of this specific Agent work is performed - it all must be done using the
 * client API. The server implementation only manages the Agent's data: its roles
 * and persistent Jobs.
 * 
 * <p>This package contains the classes for the implementation of
 * Agents on the CRISTAL server. They correspond to the Item implementations in
 * the parent package.
 * <p>This package also contains the {@link org.cristalise.kernel.entity.agent.Job} object implementation, as well
 * as the RemoteMap JobList, and the marshallable container {@link org.cristalise.kernel.entity.agent.JobArrayList}.
 * 
 */
package org.cristalise.kernel.entity.agent;