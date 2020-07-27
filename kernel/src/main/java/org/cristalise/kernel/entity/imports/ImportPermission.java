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
package org.cristalise.kernel.entity.imports;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor @AllArgsConstructor@Getter @Setter @Slf4j
public class ImportPermission {
    private String domains;
    private String actions;
    private String targets;

    /**
     * Parse string following the rules described here: https://shiro.apache.org/permissions.html
     * 
     * @param permission Wildcard Permission string of Shiro
     */
    public ImportPermission(String permission) {
        if (StringUtils.isNotBlank(permission)) {
            String[] values = permission.split(":");

            if (values.length <= 3) {
                domains = values[0];
                if (values.length > 1) actions = values[1];
                if (values.length > 2) targets = values[2];
            }
            else {
                log.warn("constructor() - permission string '{}' conatians more than 2 colons", permission);
            }
        }
        else {
            log.warn("constructor() - Permission string was null or empty");
        }
    }

    public ImportPermission(Map<String, String> permissions) {
        if (permissions.containsKey("domains")) {
            domains = permissions.get("domains");
        }
        if (permissions.containsKey("actions")) {
            actions = permissions.get("actions");
        }
        if (permissions.containsKey("targets")) {
            targets = permissions.get("targets");
        }
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();

        if (StringUtils.isNotBlank(domains)) {
            b.append(domains);
        }

        if (StringUtils.isNotBlank(actions)) {
            if (b.length() == 0) b.append("*");

            b.append(":").append(actions);
        }

        if (StringUtils.isNotBlank(targets)) {
            if      (b.length() == 0)              b.append("*:*");
            else if (StringUtils.isBlank(actions)) b.append(":*");

            b.append(":").append(targets);
        }

        return b.toString();
    }
}
