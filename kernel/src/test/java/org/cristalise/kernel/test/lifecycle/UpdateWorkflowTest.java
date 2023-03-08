package org.cristalise.kernel.test.lifecycle;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.UpdateWorkflowFromDescription;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorageManager;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UpdateWorkflowTest {
    static ItemPath itemPath;

    @BeforeAll
    public static void beforeClass() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mStorage", new ClusterStorageManager(null), true);
        itemPath = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");
    }
    

    @AfterAll
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    private UpdateWorkflowFromDescription mockUpdateWorkflow(Workflow wf) {
        UpdateWorkflowFromDescription pStep = spy(UpdateWorkflowFromDescription.class);

        when(pStep.getParent()).thenReturn(wf);

        return pStep;
    }

    @Test
    public void test() throws Exception {
        String wfXML = FileStringUtility.url2String(this.getClass().getResource("/NestedWorkflow.xml"));
        Workflow wf = (Workflow) Gateway.getMarshaller().unmarshall(wfXML);
        String domainCAXml = Gateway.getMarshaller().marshall(wf.search("workflow/domain"));

        mockUpdateWorkflow(wf).request(new AgentPath(itemPath, "kovax"), itemPath, domainCAXml, null);
    }
}
