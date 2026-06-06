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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;

import javax.annotation.concurrent.Immutable;

@Immutable
public class RolePath extends Path {
    private boolean hasJobList = false;

    //LinkedHashSet is used to make permissions string unique and to keep its original order
    private Set<String> permissions = new LinkedHashSet<>();

    public RolePath() {
        super();
    }

    public RolePath(String path) {
        super(path);
    }

    public RolePath(RolePath parent, String roleName) {
        super(parent, roleName);
    }

    public RolePath(RolePath parent, String roleName, Set<String> newPermissions) {
        this(parent, roleName);
        setPermissions(newPermissions);
    }

    public RolePath(RolePath parent, String roleName, List<String> newPermissions) {
        this(parent, roleName, newPermissions != null ? new LinkedHashSet<>(newPermissions) : null);
    }

    public RolePath(String path, boolean jobList) {
        super(path);
        this.hasJobList = jobList;
    }

    public RolePath(String path, boolean jobList, Set<String> newPermissions) {
        this(path, jobList);
        setPermissions(newPermissions);
    }
    
    public RolePath(String path, boolean jobList, List<String> newPermissions) {
        this(path, jobList, newPermissions != null ? new LinkedHashSet<>(newPermissions) : null);
    }

    public RolePath(String[] path, boolean jobList) {
        super(path);
        this.hasJobList = jobList;
    }

    public RolePath(String[] path, boolean jobList, Set<String> newPermissions) {
        this(path, jobList);
        setPermissions(newPermissions);
    }

    public RolePath(String[] path, boolean jobList, List<String> newPermissions) {
        this(path, jobList);
        setPermissions(newPermissions);
    }

    public RolePath(RolePath parent, String roleName, boolean jobList) {
        this(parent, roleName);
        this.hasJobList = jobList;
    }

    public RolePath(RolePath parent, String roleName, boolean jobList, Set<String> newPermissions) {
        this(parent, roleName, jobList);
        setPermissions(newPermissions);
    }

    public RolePath(RolePath parent, String roleName, boolean jobList, List<String> newPermissions) {
        this(parent, roleName, jobList);
        setPermissions(newPermissions);
    }

    @JsonIgnore
    public RolePath getParent() throws ObjectNotFoundException {
        return getParent(null);
    }

    @JsonIgnore
    public RolePath getParent(TransactionKey transactionKey) throws ObjectNotFoundException {
        if (mPath.length < 2) return null;

        return Gateway.getLookup().getRolePath(mPath[mPath.length - 2], transactionKey);
    }

    /**
     * @return Returns the hasJobList.
     */
    @JsonProperty( "hasJobList")
    public boolean hasJobList() {
        return hasJobList;
    }

    /**
     * @param hasJobList The hasJobList to set.
     */
    @JsonProperty("hasJobList")
    public void setHasJobList(boolean hasJobList) {
        this.hasJobList = hasJobList;
    }

    @JsonProperty("permissions")
    public List<String> getPermissionsList() {
        return new ArrayList<>(permissions);
    }

    @JsonProperty("permissions")
    public void setPermissions(List<String> newPermissions) {
        if (newPermissions != null) {
            this.permissions.clear();
            this.permissions.addAll(newPermissions);
        }
    }

    @JsonIgnore
    public Set<String> getPermissions() {
        return this.permissions;
    }

    @JsonIgnore
    public void setPermissions(Set<String> newPermissions) {
        if (newPermissions != null) {
            this.permissions.clear();
            this.permissions.addAll(newPermissions);
        }
    }

    @JsonIgnore
    public Iterator<Path> getChildren() {
        return getChildren(null);
    }

    @JsonIgnore
    public Iterator<Path> getChildren(TransactionKey transactionKey) {
        if (Gateway.getLookup() == null) return null;
        return Gateway.getLookup().getChildren((RolePath)this, transactionKey);
    }

    @Override
    public String dump() {
        StringBuilder dump = new StringBuilder("RolePath: {\n");

        dump.append("  Path:").append(this).append("\n");
        dump.append("  JobList:").append(hasJobList).append("\n");

        for (String p: permissions) dump.append("  Permission:").append(p).append("\n");

        dump.append("}\n");

        return dump.toString();
    }

    @JsonIgnore
    @Override
    public String getRoot() {
        return "role";
    }

    @JsonIgnore
    @Override
    public String getName() {
        if (mPath.length > 0) return mPath[mPath.length - 1];
        else                  return getRoot();
    }

    @JsonIgnore
    @Override
    public String getClusterPath() {
        return ClusterType.PATH + "/Role/" + StringUtils.join(mPath, "");
    }
}
