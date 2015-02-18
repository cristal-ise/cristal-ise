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
    
    public InMemoryLookupManager() {
        
    }

    @Override
    public void initializeDirectory() throws ObjectNotFoundException {
        Logger.msg(8, "InMemoryLookupManager.initializeDirectory() - Do nothing");
    }

    @Override
    public void add(Path newPath) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        Logger.msg(5, "InMemoryLookupManager.add() - Path: $newPath.string");

        if(! cache.containsKey(newPath.getString())) {
            cache[newPath.getString()] = newPath
        }
        else {
            throw new ObjectAlreadyExistsException()
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
