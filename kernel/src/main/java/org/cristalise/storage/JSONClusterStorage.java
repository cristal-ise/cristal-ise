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
package org.cristalise.storage;

import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;

/**
 * Implementation of ClusterStorage providing the JSON file based persistence.
 * Non-outcome objects are stored as direct serialized JSON instances.
 */
public class JSONClusterStorage extends FileBasedClusterStorage {
    private static final String DEFAULT_FILE_EXTENSION = ".json";

    /**
     * Required during ClusterStorageManager initialization
     */
    public JSONClusterStorage() {
        super(null, null, null, DEFAULT_FILE_EXTENSION);
    }

    public JSONClusterStorage(String root) {
        this(root, null, null);
    }

    public JSONClusterStorage(String root, String ext, Boolean useDir) {
        super(root, ext, useDir, DEFAULT_FILE_EXTENSION);
    }

    @Override
    public String getName() {
        return "JSON File Cluster Storage";
    }

    @Override
    public String getId() {
        return "JSON";
    }

    @Override
    protected boolean returnNullForMissingClusterFile() {
        return true;
    }

    @Override
    protected String serializeOutcome(Outcome outcome) {
        return outcome.getData();
    }

    @Override
    protected String serializeObject(C2KLocalObject obj) throws Exception {
        return Gateway.getMarshaller().marshall(obj);
    }

    @Override
    protected C2KLocalObject deserializeObject(String serializedData) throws Exception {
        return (C2KLocalObject) Gateway.getMarshaller().unmarshall(serializedData);
    }
}
