package org.cristalise.kernel.test.lifecycle;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.ReplaceDomainWorkflow;
import org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorageManager;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateWorkflowTest {
    static ItemPath itemPath;

    @BeforeAll
    public static void beforeClass() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/inMemoryServer.conf").getPath());
        Gateway.init(props);
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mStorage", new ClusterStorageManager(null), true);
//        itemPath = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");
        itemPath = new ItemPath("2b330968-dabb-11ed-afa1-0242ac120002");

        BulkImport importer = new BulkImport("src/test/data/xmlstorage/filebased");
        importer.initialise();
        importer.importAllClusters(itemPath, null);
    }
    

    @AfterAll
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    /**
     * Compares 2 XML string
     *
     * @param expected the reference XML
     * @param actual the xml under test
     * @return whether the two XMLs are identical or not
     */
    private static boolean compareXML(String expected, String actual)  {
        Diff diffIdentical = DiffBuilder.compare(expected).withTest(actual)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreComments()
                .ignoreWhitespace()
                .checkForSimilar()
                .build();

        if(diffIdentical.hasDifferences()) {
            log.warn(diffIdentical.toString());
            log.info("expected:\n{}", expected);
            log.info("actual:\n{}", actual);
        }

        return !diffIdentical.hasDifferences();
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
