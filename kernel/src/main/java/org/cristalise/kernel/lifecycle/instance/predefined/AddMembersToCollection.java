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
package org.cristalise.kernel.lifecycle.instance.predefined;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;

import lombok.extern.slf4j.Slf4j;
import org.cristalise.kernel.common.*;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;

@Slf4j
public class AddMembersToCollection extends AddMemberToCollection {

    public AddMembersToCollection(){
        super();
        this.setBuiltInProperty(SCHEMA_NAME, "BulkAddMembers");
    }

    //Creates a new member slot for the given item in a dependency, and assigns the item
    public static final String description = "Adds members to a given item";
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException, ObjectNotFoundException,
            InvalidCollectionModification{
        //TODO: Implement logic
        return null;
    }


    public static String bundleData(String[] data) {
        //TODO: Implement bundleData for BulkAddMembers schema
        return null;
    }
}
