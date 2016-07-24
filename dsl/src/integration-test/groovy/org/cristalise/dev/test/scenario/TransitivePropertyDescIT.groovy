package org.cristalise.dev.test.scenario;

import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test


/**
 * 
 *
 */
class TransitivePropertyDescIT extends KernelScenarioTestBase {

    @Test
    public void execute() {
        //1. Cretae new AssetDesc Item add MasterSchema and instantiate the AssetDesc
        //Check the instantiated Schema collection that is contaisn the Type property as a ClassIdentifier'
        //Edit PropertyDesc of TestSchemaFactory to have a new property which is not transitive 
        //Instantiate again the AssetDesc and check the new property is NOT in the Schema collection
        //Edit PropertyDesc of TestSchemaFactory to have a new property which is transitive
        //Instantiate again the AssetDesc and check the new property is IN  the Schema collection
    }
}
