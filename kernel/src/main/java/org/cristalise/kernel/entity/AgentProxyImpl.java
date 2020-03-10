package org.cristalise.kernel.entity;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.scripting.ScriptErrorException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgentProxyImpl extends AgentProxy
{
    private Object locker;

    public AgentProxyImpl(AgentPath agentPath, Object locker) throws ObjectNotFoundException
    {
        super(null, agentPath);
        this.locker = locker;
    }

    @Override
    public String execute(Job job)
            throws AccessRightsException,
                InvalidDataException,
                InvalidTransitionException,
                ObjectNotFoundException,
                PersistencyException,
                ObjectAlreadyExistsException,
                ScriptErrorException,
                InvalidCollectionModification
    {
        return super.execute(job);
    }
}
