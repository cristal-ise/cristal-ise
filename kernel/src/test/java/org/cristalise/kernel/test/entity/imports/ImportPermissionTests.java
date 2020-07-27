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
package org.cristalise.kernel.test.entity.imports;

import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.cristalise.kernel.entity.imports.ImportPermission;
import org.cristalise.kernel.entity.imports.ImportRole;
import org.cristalise.kernel.lookup.RolePath;
import org.junit.Test;

public class ImportPermissionTests {

    @Test
    public void testToString() {
        ImportPermission ip = new ImportPermission("domain", "", "");
        assertEquals("domain", ip.toString());

        ip.setActions("action");
        assertEquals("domain:action", ip.toString());

        ip.setTargets("target");
        assertEquals("domain:action:target", ip.toString());

        ip.setDomains("");
        assertEquals("*:action:target", ip.toString());

        ip.setActions("");
        assertEquals("*:*:target", ip.toString());

        ip.setDomains("domain");
        assertEquals("domain:*:target", ip.toString());
    }

    @Test
    public void testParseString() {
        ImportPermission ip = new ImportPermission("domain");
        assertEquals("domain", ip.toString());

        ip = new ImportPermission("domain:action");
        assertEquals("domain:action", ip.toString());

        ip = new ImportPermission("domain:action:target");
        assertEquals("domain:action:target", ip.toString());

        ip = new ImportPermission("*:action:target");
        assertEquals("*:action:target", ip.toString());

        ip = new ImportPermission("*:*:target");
        assertEquals("*:*:target", ip.toString());

        ip = new ImportPermission("domain:*:target");
        assertEquals("domain:*:target", ip.toString());
    }
    
    @Test
    public void testAdminRole() {
        RolePath adminRole = new RolePath(new RolePath(), ADMIN_ROLE.getName(), false, Arrays.asList("*"));
        ImportRole importAdminRole = ImportRole.getImportRole(adminRole);
        
        assertEquals("*", importAdminRole.permissions.get(0).toString());
    }

}
