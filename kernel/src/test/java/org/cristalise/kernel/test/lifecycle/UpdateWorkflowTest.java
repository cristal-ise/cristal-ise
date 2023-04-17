package org.cristalise.kernel.test.lifecycle;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.ReplaceDomainWorkflow;
import org.cristalise.kernel.lifecycle.instance.predefined.UpdateWorkflowFromDescription;
import org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorageManager;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.TestUtility;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateWorkflowTest implements TestUtility {
    ItemPath itemPath;

    @BeforeEach
    public void before() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/inMemoryServer.conf").getPath());
        Gateway.init(props);
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mStorage", new ClusterStorageManager(null), true);
        itemPath = new ItemPath("2b330968-dabb-11ed-afa1-0242ac120002");

        BulkImport importer = new BulkImport("src/test/data/xmlstorage/filebased");
        importer.initialise();
        importer.importAllClusters(itemPath, null);
    }

    @AfterEach
    public void after() throws Exception {
        Gateway.close();
    }


    private ReplaceDomainWorkflow mockReplaceDomainWorkflow(Workflow wf) throws Exception {
        ReplaceDomainWorkflow pStep = spy(ReplaceDomainWorkflow.class);

        when(pStep.getParent()).thenReturn(wf);

        return pStep;
    }

    @Test
    public void replaceDomainWorkflow() throws Exception {
        Workflow wf = (Workflow) Gateway.getStorage().get(itemPath, ClusterType.LIFECYCLE + "/workflow");
        CompositeActivity origDomainCA = (CompositeActivity)wf.search("workflow/domain");

        String newWfXML = FileStringUtility.url2String(this.getClass().getResource("/NestedWorkflow.xml"));
        Workflow newWf = (Workflow) Gateway.getMarshaller().unmarshall(newWfXML);
        String newDomainCAXml = Gateway.getMarshaller().marshall(newWf.search("workflow/domain"));

        String requestData = "<WorkflowReplaceData><NewWorkflowXml>"+newDomainCAXml+"</NewWorkflowXml><OldWorkflowXml/></WorkflowReplaceData>";

        String resultXml = mockReplaceDomainWorkflow(wf).request(new AgentPath(itemPath, "kovax"), itemPath, requestData, null);

        Outcome resultWorkflowReplaceData = new Outcome(resultXml);
        String resultOldDomainCAXml = Outcome.serialize(resultWorkflowReplaceData.getNodeByXPath("//OldWorkflowXml/CompositeActivity"), false);

        assert compareXML(
                Gateway.getMarshaller().marshall(origDomainCA), 
                resultOldDomainCAXml
        );
    }
}
