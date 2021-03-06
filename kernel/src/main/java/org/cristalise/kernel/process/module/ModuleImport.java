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
package org.cristalise.kernel.process.module;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.resource.ResourceImportHandler.Status;
import org.cristalise.kernel.persistency.TransactionKey;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class ModuleImport {

    protected String     ns;
    protected String     name;
    protected DomainPath domainPath;
    protected ItemPath   itemPath;

    protected boolean isNewItem = true;
    protected boolean isDOMPathExists = true; //avoids multiple call to domainPath.exists()

    protected String resourceChangeDetails = null;
    protected Status resourceChangeStatus = null;

    public ModuleImport() {}

    public ItemPath getItemPath(TransactionKey transactionKey) { return itemPath; };

    public abstract Path create(AgentPath agentPath, boolean reset, TransactionKey transactionKey)
            throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, ObjectAlreadyExistsException,
                   InvalidCollectionModification, InvalidDataException, AccessRightsException, PersistencyException;

    public void setID(String uuid) throws InvalidItemPathException {
        if (StringUtils.isNotBlank(uuid)) itemPath = new ItemPath(uuid);
    }

    public String getID() {
        return itemPath == null ? null : itemPath.getUUID().toString();
    }

    public void setNamespace(String ns) {
        this.ns = ns;
    }

    public String getNamespace() {
        return ns;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + (ns == null ? 0 : ns.hashCode());
    }
}