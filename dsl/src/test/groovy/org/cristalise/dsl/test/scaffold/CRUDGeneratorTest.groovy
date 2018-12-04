package org.cristalise.dsl.test.scaffold

import org.cristalise.dsl.scaffold.CRUDGenerator
import org.junit.Test

class CRUDGeneratorTest {

    @Test
    void generateTestItem( ) {
        def vars = [rootDir: 'src/test', moduleName: 'Test Module', moduleNs: 'testns', moduleVersion: 0, item: 'TestItem']
        new CRUDGenerator().generate(vars, true, true)
    }
}
