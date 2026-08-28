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
package org.cristalise.kernel.test.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LifecycleSearchVertexTest {

    static Workflow wf = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(LifecycleSearchVertexTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);

        String wfXML = FileStringUtility.url2String(LifecycleSearchVertexTest.class.getResource("/NestedWorkflow.xml"));
        wf = (Workflow) Gateway.getMarshaller().unmarshall(wfXML);
    }

    @Test
    public void seacrhByNameFromRoot() throws Exception {
        assertEquals(CompositeActivity.class, wf.search("workflow/domain").getClass());
        assertEquals(CompositeActivity.class, wf.search("workflow/domain/DomainWorkflow/StartSequence").getClass());
        assertEquals(Activity.class, wf.search("workflow/domain/DomainWorkflow/StartSequence/Pull").getClass());

        assertEquals(wf.search("domain"), wf.search("workflow/domain"));
        assertEquals(
                wf.search(         "domain/DomainWorkflow/StartSequence/Pull"),
                wf.search("workflow/domain/DomainWorkflow/StartSequence/Pull")
        );
    }

    @Test
    public void seacrhByNameFromDomainWorkflow() throws Exception {
        CompositeActivity domainWorkflowCA = (CompositeActivity)wf.search("workflow/domain/DomainWorkflow");

        assertEquals(CompositeActivity.class, domainWorkflowCA.search("DomainWorkflow/StartSequence").getClass());
        assertEquals(CompositeActivity.class, domainWorkflowCA.search(               "StartSequence").getClass());
        assertEquals(Activity.class,          domainWorkflowCA.search("DomainWorkflow/StartSequence/Pull").getClass());
        assertEquals(Activity.class,          domainWorkflowCA.search(               "StartSequence/Pull").getClass());

        assertEquals(
                domainWorkflowCA.search(               "StartSequence"),
                domainWorkflowCA.search("DomainWorkflow/StartSequence")
        );
        assertEquals(
                domainWorkflowCA.search(               "StartSequence/Pull"),
                domainWorkflowCA.search("DomainWorkflow/StartSequence/Pull")
        );
    }

    public void seacrhByIdFromRoot() throws Exception {
        assertEquals(wf.search("0"), wf.search("-1/0"));
        assertEquals(
                wf.search("-1/0/15/10/0"),
                wf.search("workflow/domain/DomainWorkflow/StartSequence/Pull")
        );

        CompositeActivity domainWorkflowCA = (CompositeActivity)wf.search("workflow/domain/DomainWorkflow");
        assertEquals(
                domainWorkflowCA.search("10/0"),
                domainWorkflowCA.search("StartSequence/Pull")
        );
        assertEquals(
                domainWorkflowCA.search("15/10/0"),
                domainWorkflowCA.search("DomainWorkflow/StartSequence/Pull")
        );
    }
}
