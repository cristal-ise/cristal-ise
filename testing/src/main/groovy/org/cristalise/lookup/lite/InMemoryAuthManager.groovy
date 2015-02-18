package org.cristalise.lookup.lite

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.process.auth.Authenticator
import org.cristalise.kernel.utils.Logger

class InMemoryAuthManager implements Authenticator {

    @Override
    public boolean authenticate(String agentName, String password, String resource) throws InvalidDataException, ObjectNotFoundException {
        Logger.warning("InMemoryAuthManager.authenticate() - name:$agentName - This implemetation ALWAYS returns true!");
        return true;
    }

    @Override
    public boolean authenticate(String resource) throws InvalidDataException, ObjectNotFoundException {
        Logger.warning("InMemoryAuthManager.authenticate() - resource:$resource - This implemetation ALWAYS returns true!");
        return true;
    }

    @Override
    public Object getAuthObject() {
        Logger.warning("InMemoryAuthManager.getAuthObject() - This implemetation ALWAYS returns null!");
        return null;
    }

    @Override
    public void disconnect() {
        Logger.msg(8, "InMemoryAuthManager.disconnect() - Do nothing");
    }
}
