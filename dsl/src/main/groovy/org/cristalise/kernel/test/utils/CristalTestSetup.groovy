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
package org.cristalise.kernel.test.utils

import java.util.Properties
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Bootstrap
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.auth.Authenticator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 *
 */
@CompileStatic
trait CristalTestSetup {
    // @Slf4j annotation does not work on traits
    private static final Logger log = LoggerFactory.getLogger(this.class)

    private void waitBootstrapThread() {
        //Give some time the Bootstrapper thread to start so this check will not fail because it was executed prematurely
        Thread.sleep(1000)

        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ( ( parentGroup = rootGroup.getParent() ) != null ) { rootGroup = parentGroup; }

        Thread[] threads = new Thread[ rootGroup.activeCount() ];
        while ( rootGroup.enumerate(threads) == threads.length ) {
            threads = new Thread[ threads.length * 2 ];
        }

        Thread bootstrapT = null
        int index = threads.length-1

        while(bootstrapT == null && index >= 0 ) {
            if(threads[index] && threads[index].getName() == "Bootstrapper") { bootstrapT = threads[index] }
            index--
        }

        if (bootstrapT) {
            log.info('waitBootstrapThread() - Bootstrapper FOUND')
            bootstrapT.join()
            log.info "waitBootstrapThread() - Bootstrapper FINISHED"
        }
        else {
            log.error "waitBootstrapThread() - NO Bootstrapper FOUND!?!?"
            AbstractMain.shutdown(1) 
        }
    }

    public void inMemorySetup(String conf, String clc) {
        def testProps = new Properties()
        testProps.put('Gateway.clusteredVertx', false);

        cristalSetup(conf, clc, testProps)
        //FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookupManager", Gateway.getLookup(), true)
    }

    public void inMemorySetup() {
        inMemorySetup('src/test/conf/testServer.conf', 'src/test/conf/testInMemory.clc')
    }

    public void inMemoryServer(String conf, String clc, Properties testProps = null, boolean skipBootstrap = false) {
        if (!testProps) testProps = new Properties()
        testProps.put('Gateway.clusteredVertx', true);

        serverSetup(conf, clc, testProps, skipBootstrap)
    }

    public void inMemoryServer(Properties testProps = null, boolean skipBootstrap = false) {
        inMemoryServer('src/test/conf/testServer.conf', 'src/test/conf/testInMemory.clc', testProps, skipBootstrap)
    }

    public void serverSetup(String config, String connect, Properties testProps = null, boolean skipBootstrap = false) {
        AbstractMain.isServer = true

        if (skipBootstrap) {
            if (testProps == null) testProps = new Properties()
            testProps.put(AbstractMain.MAIN_ARG_SKIPBOOTSTRAP, true)
        }
        Authenticator auth = cristalSetup(config, connect, testProps)

        Gateway.startServer()
        Gateway.runBoostrap();

        if (!skipBootstrap) {
            waitBootstrapThread()
        }
    }

    public void cristalSetup(String config, String connect, Properties testProps = null) {
        cristalInit(config, connect, testProps)
        Gateway.connect()
    }

    public void cristalInit(String config, String connect, Properties testProps = null) {
        if (testProps == null) testProps = new Properties();
        if (!testProps.containsKey('Shiro.iniFile')) testProps.put("Shiro.iniFile", "src/main/bin/shiro.ini");

        Gateway.init(AbstractMain.readPropertyFiles(config, connect, testProps))
    }

    public static void cristalCleanup() {
        Gateway.close()
    }
}
