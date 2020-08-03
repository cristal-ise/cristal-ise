package org.cristalise.dev.test.scenario;

import static org.cristalise.kernel.process.resource.BuiltInResources.ROLE_DESC_RESOURCE

import org.cristalise.kernel.entity.imports.ImportPermission
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.DefaultResourceImportHandler
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test

import groovy.transform.CompileStatic


@CompileStatic
class RoleDescIT extends KernelScenarioTestBase {
    
    public static final String testDataRoot = "src/main/data";

    @Test
    public void createAndEditRoleDesc() {
        String name = "RoleDesc-$timeStamp"
        def type = ROLE_DESC_RESOURCE
        def item = createNewDevItem(type, "CreateNewRoleDesc", name, folder)

        assert item && item.getName() == name
        assert item.getMasterSchema()
        //assert item.getAggregateScript()

        // it is not necessary to execute Edit
        //executeDoneJob(item, 'Edit', 'xml1')
        //assert item.getViewpoint('RoleDesc_Details', 'last')

        executeDoneJob(item, 'EditPermission', Gateway.marshaller.marshall(new ImportPermission('Script:EditDefinition:*')))
        executeDoneJob(item, 'EditPermission', Gateway.marshaller.marshall(new ImportPermission('Schema:EditDefinition:*')))
        assert item.checkViewpoint('Permission', 'Script')
        assert item.checkViewpoint('Permission', 'Schema')

        executeDoneJob(item, 'CreateNewVersion')
        def importRoleXml = item.getViewpoint('Role', "0").getOutcome().getData()
        def importRole = (ImportRole)Gateway.marshaller.unmarshall(importRoleXml)
        assert 'Script:EditDefinition:*' == importRole.permissions[0].toString()
        assert 'Schema:EditDefinition:*' == importRole.permissions[1].toString()

        executeDoneJob(item, 'ApplyChanges')
    }
}
