package org.cristalise.kernel.lifecycle.instance.predefined;

import org.cristalise.kernel.common.*;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;

public class AddMembersToCollection extends AddMemberToCollection {
    //Creates a new member slot for the given item in a dependency, and assigns the item
    public static final String description = "Adds members to a given item";
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException, ObjectNotFoundException,
            InvalidCollectionModification{
        //TODO: Implement logic
        return null;
    }
}
