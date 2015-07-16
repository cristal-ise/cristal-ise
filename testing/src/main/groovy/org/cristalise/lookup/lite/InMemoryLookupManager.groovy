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
package org.cristalise.lookup.lite

import groovy.transform.CompileStatic

import java.security.NoSuchAlgorithmException

import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.common.ObjectCannotBeUpdated
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.utils.Logger


@CompileStatic
class InMemoryLookupManager extends InMemoryLookup implements LookupManager {
    
    public InMemoryLookupManager() {}

    @Override
    public void initializeDirectory() throws ObjectNotFoundException {
        Logger.msg(8, "InMemoryLookupManager.initializeDirectory() - Do nothing");
    }

    @Override
    public void add(Path newPath) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        Logger.msg(5, "InMemoryLookupManager.add() - Path: $newPath.string");

        if(cache.containsKey(newPath.getString())) { throw new ObjectAlreadyExistsException()}
        else {
            cache[newPath.getString()] = newPath
        }
    }

    @Override
    public void delete(Path path) throws ObjectCannotBeUpdated {
        Logger.msg(5, "InMemoryLookupManager.delete() - Path: $path.string");
        
        if(cache.containsKey(path.getString())) {
            cache.remove(path.getString())
        }
        else {
            throw new ObjectCannotBeUpdated()
        }
    }

    @Override
    public RolePath createRole(RolePath role) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addRole(AgentPath agent, RolePath rolePath) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setAgentPassword(AgentPath agent, String newPassword) throws ObjectNotFoundException, ObjectCannotBeUpdated,
            NoSuchAlgorithmException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHasJobList(RolePath role, boolean hasJobList) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        // TODO Auto-generated method stub

    }
}
