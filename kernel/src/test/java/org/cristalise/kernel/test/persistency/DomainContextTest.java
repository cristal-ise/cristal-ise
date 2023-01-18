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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Properties;
import org.cristalise.kernel.entity.DomainContext;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DomainContextTest {

    @BeforeAll
    public static void setup() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
    }

    @Test
    public void createDomainContext() throws Exception {
        DomainContext desc = new DomainContext("/desc");
        DomainContext descParent = desc.getParentContext();

        assertEquals("DescContext", desc.getName());
        assertNotNull(descParent);
        assertEquals("Context", descParent.getName());
    }

    @Test
    public void castorMarshall() throws Exception {
        DomainContext desc = new DomainContext("/desc");
        String descXml = Gateway.getMarshaller().marshall(desc);

        Schema domainContextSchema = LocalObjectLoader.getSchema("DomainContext", 0);
        new Outcome(descXml, domainContextSchema).validateAndCheck();

        DomainContext descPrime = (DomainContext) Gateway.getMarshaller().unmarshall(descXml);
        assertThat(desc).isEqualToComparingFieldByField(descPrime);
    }

    @Test
    public void localObjectLoader() throws Exception {
        DomainContext desc  = LocalObjectLoader.getDomainContext("DescContext", 0);
        assertEquals("DescContext", desc.getName());

        DomainContext descPropDesc = LocalObjectLoader.getDomainContext("DescPropertyDescContext", 0);
        assertEquals("DescPropertyDescContext", descPropDesc.getName());

        DomainContext descPropDescKernel = LocalObjectLoader.getDomainContext("DescPropertyDescKernelContext", 0);
        assertEquals("DescPropertyDescKernelContext", descPropDescKernel.getName());
    }
}
