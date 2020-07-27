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
