package org.cristalise.dev.test.scenario;

import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.jupiter.api.Test


/**
 *
 *
 */
class TransitivePropertyDescIT extends KernelScenarioTestBase {

    @Test
    public void execute() {
        //1. Cretae new AssetDesc Item add MasterSchema and instantiate the AssetDesc
        //2. Check the instantiated Schema collection that is contaisn the Type property as a ClassIdentifier'
        //3. Edit PropertyDesc of TestSchemaFactory to have a new property which is not transitive 
        //4. Instantiate again the AssetDesc and check the new property is NOT in the Schema collection
        //5. Edit PropertyDesc of TestSchemaFactory to have a new property which is transitive
        //6. Instantiate again the AssetDesc and check the new property is IN  the Schema collection
    }
}
