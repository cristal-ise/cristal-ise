package org.cristalise.kernel.entity;

import java.io.IOException;

import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.property.PropertyArrayList;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ItemProxyImpl extends ItemProxy
{
    protected ItemProxyImpl(ItemPath itemPath, ItemImplementation item) {
        super(null, itemPath);
        mItem = item;
    }

    @Override
    protected ItemOperations getItem() throws ObjectNotFoundException {
        return mItem;
    }

    @Override
    public void initialise(AgentPath agentId,
            PropertyArrayList itemProps,
            CompositeActivity workflow,
            CollectionArrayList colls,
            Viewpoint viewpoint,
            Outcome outcome
            )
                    throws AccessRightsException,
                    InvalidDataException,
                    PersistencyException,
                    ObjectNotFoundException,
                    MarshalException,
                    ValidationException,
                    IOException,
                    MappingException,
                    InvalidCollectionModification
    {
        throw new InvalidDataException("ItemProxyImpl.initialise not implemented");
    }

    @Override
    public void setProperty(AgentProxy agent, String name, String value)
            throws AccessRightsException, PersistencyException, InvalidDataException
            {

            }
}
