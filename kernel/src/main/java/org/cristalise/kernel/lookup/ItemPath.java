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
package org.cristalise.kernel.lookup;

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyUtility;

/**
 * Extends Path to enforce SystemKey structure and support UUID form
 */
public class ItemPath extends Path {
    
    String itemName;

    public ItemPath() {
        setSysKey(UUID.randomUUID());
    }

    public ItemPath(UUID uuid) {
        setSysKey(uuid);
    }

    public ItemPath(String[] path) throws InvalidItemPathException {
        super(path);
        checkSysKeyFromPath();
    }

    public ItemPath(String path) throws InvalidItemPathException {
        super(path);

        if (path == null) throw new InvalidItemPathException("Path cannot be null");

        checkSysKeyFromPath();
    }

    @Override
    public void setPath(String[] path) {
        super.setPath(path);
    }

    @Override
    public void setPath(String path) {
        super.setPath(path);
    }

    @Override
    public void setPath(Path path) {
        super.setPath(path);
    }

    private void checkSysKeyFromPath() throws InvalidItemPathException {
        if (mPath.length == 1) {
            try {
                getUUID();
            }
            catch (Throwable ex) {
                throw new InvalidItemPathException(mPath[0] + " is not a valid UUID : " + ex.getMessage());
            }
        }
        else
            throw new InvalidItemPathException("Not a valid item path: " + Arrays.toString(mPath));
    }

    /**
     * The root of ItemPath is /entity
     */
    @Override
    public String getRoot() {
        return "entity";
    }

    @Override
    public ItemPath getItemPath(TransactionKey transactionKey) throws ObjectNotFoundException {
        return this;
    }

    public static ItemPath getItemPath(String pathOrUUID, TransactionKey transactionKey) throws ObjectNotFoundException {
        Validate.notBlank(pathOrUUID);

        ItemPath ip = null;

        try {
            if (ItemPath.isUUID(pathOrUUID)) ip = Gateway.getLookup().getItemPath(pathOrUUID, transactionKey);
            else                             ip = Gateway.getLookup().resolvePath(new DomainPath(pathOrUUID), transactionKey);
        }
        catch (InvalidItemPathException e) {
            throw new ObjectNotFoundException(e);
        }

        return ip;
    }

    public byte[] getOID() {
        UUID uuid = getUUID();

        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        return bb.array();
    }

    protected void setSysKey(UUID uuid) {
        setPathFromUUID(uuid);
    }

    private void setPathFromUUID(UUID uuid) {
        mPath = new String[1];
        mPath[0] = uuid.toString();
    }

    @Override
    public UUID getUUID() {
        return UUID.fromString(mPath[0]);
    }

    /**
     * Returns the UUID in String form
     */
    @Override
    public String getName() {
        return mPath[0]; //originally it was 'return getUUID().toString()';
    }

    @Override
    public String getClusterPath() {
        return ClusterType.PATH + "/Item";
    }

    /**
     * Check if the given string contains a UUID. Remove the cristal-ise specific '/entity/' prefix if present
     * @param entityKey the entity key of an Item, can contain the '/entity/' prefix
     * @return whether the entytyKey is a UUID or not
     */
    public static boolean isUUID(String entityKey) {
        if (entityKey.startsWith("/entity/")) entityKey = entityKey.substring(8);

        return entityKey.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    public String getItemName() {
        return getItemName(null);
    }

    public String getItemName(TransactionKey transactionKey) {
        if (StringUtils.isBlank(itemName)) {
            itemName = PropertyUtility.getPropertyValue(this, NAME, "", transactionKey);
        }

        return itemName;
    }
}
