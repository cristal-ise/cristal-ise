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
package org.cristalise.kernel.test.persistency;

import static org.cristalise.kernel.persistency.ClusterType.PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.storage.MemoryOnlyClusterStorage;
import org.junit.Before;
import org.junit.Test;

public class LookupPathClusterStorageTest {

    ItemPath storingItem = new ItemPath();
    ClusterStorage inMemoryCluster = new MemoryOnlyClusterStorage();

    @Before
    public void setup() throws Exception {
        inMemoryCluster.open(null);
    }

    @Test
    public void storeItemPath() throws Exception {
        ItemPath item = new ItemPath(UUID.randomUUID());

        inMemoryCluster.put(storingItem, item, null);

        ItemPath itemPrime = (ItemPath) inMemoryCluster.get(storingItem, PATH + "/Item", null);

        assertNotNull(itemPrime);
        assertEquals(item.getUUID(),      itemPrime.getUUID());
    }

    @Test
    public void storeAgentPath() throws Exception {
        AgentPath agent = new AgentPath(UUID.randomUUID(), "toto");

        inMemoryCluster.put(storingItem, agent, null);

        AgentPath agentPrime = (AgentPath) inMemoryCluster.get(storingItem, PATH + "/Agent", null);

        assertNotNull(agentPrime);
        assertEquals(agent.getUUID(),      agentPrime.getUUID());
        assertEquals(agent.getAgentName(), agentPrime.getAgentName());
    }

    @Test
    public void storeDomainPath() throws Exception {
        DomainPath domain = new DomainPath("/my/path.2", new ItemPath());

        inMemoryCluster.put(storingItem, domain, null);
        
        String name = StringUtils.remove( StringUtils.join(domain.getPath(), ""), "." );

        DomainPath domainPrime = (DomainPath) inMemoryCluster.get(storingItem, PATH + "/Domain/" + name, null);

        assertNotNull(domainPrime);
        assertEquals(domain.getStringPath(), domainPrime.getStringPath());
        assertEquals(domain.getTargetUUID(), domainPrime.getTargetUUID());
    }

    @Test
    public void storeRolePath() throws Exception {
        RolePath role      = new RolePath("Minion", false);

        inMemoryCluster.put(storingItem, role, null);

        RolePath rolePrime = (RolePath) inMemoryCluster.get(storingItem, PATH + "/Role/" + StringUtils.join(role.getPath(), ""), null);

        assertNotNull(rolePrime);
        assertEquals(role.getStringPath(), rolePrime.getStringPath());
        assertEquals(role.hasJobList(), rolePrime.hasJobList());
    }
    
    @Test
    public void checkUUID() throws Exception {
        assert ItemPath.isUUID("00000000-0000-0000-0000-000000000000");
        assert ItemPath.isUUID("30923dec-c881-40b9-86fc-075c91592ebe");
        assert ItemPath.isUUID("/entity/30923dec-c881-40b9-86fc-075c91592ebe");
    }

}
