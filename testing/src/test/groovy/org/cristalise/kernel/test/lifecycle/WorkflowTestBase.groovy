package org.cristalise.kernel.test.lifecycle;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.junit.After
import org.junit.Before


@CompileStatic
class WorkflowTestBase {
    
    @Before
    public void init() {
        String[] args = ['-logLevel', '8', '-config', 'src/test/conf/testServer.conf', '-connect', 'src/test/conf/testInMemory.clc']
        Gateway.init(AbstractMain.readC2KArgs(args))
        Gateway.connect()
    }

    @After
    public void tearDown() {
        Gateway.close()
    }
}
