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
package org.cristalise.kernel.collection;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;

import java.util.ArrayList;
import java.util.List;

import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.graph.model.GraphModel;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.utils.CastorHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * Collections are Item local objects that reference other Items.
 * 
 * <p>
 * In parallel with the OO meta-model, Items can be linked to other Items in different ways. These links are modelled with Collections,
 * which are local objects stored in an Item which reference a number of other Items in the same server. The Collections holds a
 * CollectionMember, sometimes known as a slot, to reference each Item and store additional information about the link.
 * 
 * <p>
 * Features:
 * <ul>
 * <li><b>Typing</b> - Collections can restrict membership of based on type information derived from Item, Property and Collection
 * descriptions. This restriction may be per-slot or apply to the whole Collection.
 * 
 * <li><b>Fixed or flexible slots</b> - The CollectionMember objects of a Collection may be empty, individually typed, or created and
 * removed as required, simulating either array, structures or lists.
 * 
 * <li><b>Layout</b> - Collections can include a {@link GraphModel} to lay out its slots on a two-dimensional canvas, for modelling real
 * world compositions.
 * </ul>
 * 
 * <p>
 * Collections are managed through predefined steps.
 */
@Slf4j
abstract public class Collection<E extends CollectionMember> implements C2KLocalObject {
    public enum Type {Bidirectional, Unidirectional}
    public enum Cardinality {
        OneToMany, ManyToOne, OneToOne, ManyToMany;

        public Cardinality reverse() {
            switch (this) {
                case OneToOne:
                case ManyToMany:
                    return this;

                case OneToMany:
                    return ManyToOne;

                case ManyToOne:
                    return OneToMany;

                default:
                    log.warn("reverse() - unrecognised value:{}", this);
                    break;
            }
            return null;
        }
    }

    private int                       mCounter = -1;   // Contains next available Member ID
    protected CollectionMemberList<E> mMembers = new CollectionMemberList<E>();
    protected String                  mName    = "";   // Not checked for uniqueness
    protected Integer                 mVersion = null;

    /**
     * Fetch the current highest member ID of the collection. This is found by scanning all the current members
     * and kept in the mCounter field, but is not persistent.
     * 
     * @return the current highest member ID
     */
    public int getCounter() {
        if (mCounter == -1) {
            for (E element : mMembers.list) {
                if (mCounter < element.getID()) mCounter = element.getID();
            }
        }
        return ++mCounter;
    }

    /**
     * @return The total number of slots in this collection, including empty ones
     */
    public int size() {
        return mMembers.list.size();
    }

    /**
     * Sets the collection name
     */
    @Override
    public void setName(String name) {
        mName = name;
    }

    /**
     * @return The collection's name
     */
    @Override
    public String getName() {
        return mName;
    }

    /**
     * Get the collection version. Null if not set, and will be stored as 'last'
     * 
     * @return Integer version
     */
    public Integer getVersion() {
        return mVersion;
    }

    /**
     * Set a named version for this collection. Must be an integer or null. Named versions will be stored separately to the current version
     * ('last') and should not change once saved.
     * 
     * @param version the version to set
     */
    public void setVersion(Integer version) {
        this.mVersion = version;
    }

    /**
     * Get the version name for storage, which is 'last' unless the version number is set.
     * 
     * @return String
     */
    public String getVersionName() {
        return mVersion == null ? "last" : String.valueOf(mVersion);
    }

    @Override
    public ClusterType getClusterType() {
        return ClusterType.COLLECTION;
    }

    @Override
    public String getClusterPath() {
        return getClusterType()+"/"+mName+"/"+getVersionName();
    }

    public void setMembers(CollectionMemberList<E> newMembers) {
        mMembers = newMembers;
    }

    public boolean contains(ItemPath itemPath) {
        for (E element : mMembers.list) {
            if (element.getItemPath().equals(itemPath)) return true;
        }
        return false;
    }

    /**
     * Gets the description version referenced by the given collection member. Assumes 'last' if version not given.
     * 
     * @param mem
     *            The member in question
     * @return String version tag
     */
    public String getDescVer(E mem) {
        String descVer = "last";
        Object descVerObj = mem.getProperties().getBuiltInProperty(VERSION);
        if (descVerObj != null) descVer = descVerObj.toString();
        return descVer;
    }

    /**
     * Check if all slots have an assigned Item
     * 
     * @return boolean
     */
    public boolean isFull() {
        for (E element : mMembers.list) {
            if (element.getItemPath() == null) return false;
        }
        return true;
    }

    /**
     * Find collection member by its integer ID
     * 
     * @param memberId to find
     * @return the CollectionMember with that ID
     * @throws ObjectNotFoundException when the ID wasn't found
     */
    public E getMember(int memberId) throws ObjectNotFoundException {
        for (E element : mMembers.list) {
            if (element.getID() == memberId) return element;
        }
        throw new ObjectNotFoundException("MemberId:" + memberId + " not found in Collection:" + mName);
    }

    /**
     * Find first collection member with the given ItemPath.
     * 
     * @param itemPath to find
     * @return the CollectionMember with that ItemPath
     * @throws ObjectNotFoundException when the ID wasn't found
     */
    public E getMember(ItemPath itemPath) throws ObjectNotFoundException {
        for (E element : mMembers.list) {
            if (element.getItemPath().equals(itemPath)) return element;
        }
        throw new ObjectNotFoundException("Member " + itemPath + " not found in " + mName);
    }

    public CollectionMemberList<E> getMembers() {
        return mMembers;
    }

    /**
     * Add a member to this collection, with the given property and class properties and optionally an Item to assign, which may be null if
     * the collection allows empty slots.
     * 
     * @param itemPath
     *            the Item to assign to the new slot. Optional for collections that allow empty slots
     * @param props
     *            the Properties of the new member
     * @param classProps
     *            the names of the properties that dictate the type of assigned Items.
     * @return the new CollectionMember instance
     * @throws InvalidCollectionModification
     *             when the assignment was invalid because of collection constraints, such as global type constraints, or not allowing empty
     *             slots.
     * @throws ObjectAlreadyExistsException
     *             some collections don't allow multiple slots assigned to the same Item, and throw this Exception if it is attempted
     */
    public abstract E addMember(ItemPath itemPath, CastorHashMap props, String classProps, TransactionKey transactionKey)
            throws InvalidCollectionModification, ObjectAlreadyExistsException;

    /**
     * Removes the slot with the given ID from the collection.
     * 
     * @param memberId to remove
     * @return removed member instance
     * @throws ObjectNotFoundException when there was no slot with this ID found.
     */
    public E removeMember(int memberId) throws ObjectNotFoundException {
        List<E> members = resolveMembers(memberId);

        if (members.size() == 1) {
            mMembers.list.remove(members.get(0));
            return members.get(0);
        }
        else {
            throw new ObjectNotFoundException("Collection name:"+getName()+" has more then one MemberId:"+memberId);
        }
    }

    /**
     * Removes the slot with the given itemPath from the collection.
     * 
     * @param ip itemPath to be removed
     * @return removed member instance
     * @throws ObjectNotFoundException when there was no slot with this itemPath found.
     */
    public E removeMember(ItemPath ip) throws ObjectNotFoundException {
        List<E> members = resolveMembers(ip);

        if (members.size() == 1) {
            mMembers.list.remove(members.get(0));
            return members.get(0);
        }
        else {
            throw new ObjectNotFoundException("Collection name:"+getName()+" has more then one Member:"+ip);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mMembers == null) ? 0 : mMembers.hashCode());
        result = prime * result + ((mName == null)    ? 0 : mName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null)                  return false;
        if (getClass() != obj.getClass()) return false;

        Collection<?> other = (Collection<?>) obj;

        if (mMembers == null && other.mMembers != null) return false;
        else if (!mMembers.equals(other.mMembers))      return false;

        if (mName == null && other.mName != null) return false;
        else if (!mName.equals(other.mName))      return false;

        return true;
    }

    /**
     * Helper method to find all the members for the given item.
     * 
     * @param childPath the UUID of the item in the slots
     * @return the list of members found for the given item
     * @throws ObjectNotFoundException there is not member found for the given input parameters
     */
    public List<E> resolveMembers(ItemPath childPath) throws ObjectNotFoundException {
        return resolveMembers(-1, childPath);
    }

    /**
     * Helper method to find all the members for the given item.
     * 
     * @param slotID The id of the slot (aka memberID)
     * @return the list of members found for the given ID
     * @throws ObjectNotFoundException there is not member found for the given input parameters
     */
    public List<E> resolveMembers(int slotID) throws ObjectNotFoundException {
        return resolveMembers(slotID, null);
    }

    /**
     * Helper method to find all the members with the combination of the input parameters.
     * 
     * @param slotID The id of the slot (aka memberID). When it is set to -1, only the chilPath is used for searching.
     * @param childPath The UUID of the item in the slots. When it is set to null, only the slotID is used for searching.
     * @return the list of members found for the combination of the input parameters
     * @throws ObjectNotFoundException iff no member was found for the given input parameters. When both 
     * parameters are supplied it indicates that the given slotID does not reference the childPath.
     */
    public List<E> resolveMembers(int slotID, ItemPath childPath) throws ObjectNotFoundException {
        List<E> members = new ArrayList<>();

        if (slotID > -1) { // find the member for the given ID
            E slot = getMember(slotID);

            // if both parameters are supplied, check the given item is actually in that slot
            if (childPath != null && !slot.getItemPath().equals(childPath)) {
                throw new ObjectNotFoundException("Item:" + childPath + " was not in slot:" + slotID + " of collection:"+getName());
            }

            members.add(slot);
        }
        else { // find the members from entity key (UUID)
            for (E member: getMembers().list) {
                if (member.getItemPath().equals(childPath)) members.add(member);
            }
        }

        if (members.isEmpty()) {
            throw new ObjectNotFoundException( "Could not find Item:" + childPath + " in slot:" + slotID + " of collection:"+getName());
        }

        return members;
    }
}
