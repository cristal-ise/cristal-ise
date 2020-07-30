/*
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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

var name   = job.getOutcome().getField("Name");
var folder = job.getOutcome().getField("SubFolder");
var roles  = job.getOutcome().getField("InitialRoles");
var pwd    = job.getOutcome().getField("Password");

var root = job.getActPropString("Root");
if (root == null) root = item.getProperty("Root", null, null);

var domPath = (root != null ? root : "") + "/" + (folder != null ? folder : "");

// Create new Item
var params = new Array(4);
params[0] = name;
params[1] = domPath;
params[2] = roles;
params[3] = pwd;

try {
    agent.execute(item, "CreateAgentFromDescription", params);
} catch (e) {
    throw "Could not create "+name+": "+e.message;
}
