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
import org.cristalise.kernel.utils.Logger

import groovy.transform.CompileStatic

/**
 *
 */
@CompileStatic
trait CristalTestSetup {
    final int defaultLogLevel = 8

    private void waitBootstrapThread() {
        //Give some time the Bootstrapper to start so this check will not fail because it was executed prematurely
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

        if(bootstrapT) {
            Logger.msg "CristalTestSetup.waitBootstrapThread() - Bootstrapper FOUND"
            bootstrapT.join()
            Logger.msg "CristalTestSetup.waitBootstrapThread() - Bootstrapper FINISHED"
        }
        else
            Logger.die "CristalTestSetup.waitBootstrapThread() - NO Bootstrapper FOUND!?!?"
    }

    public void loggerSetup(int logLevel = defaultLogLevel) {
        Logger.addLogStream(System.out, logLevel);
    }

    public void loggerCleanup() {
        Logger.removeLogStream(System.out);
    }

    public void inMemorySetup(String conf, String clc,int logLevel) {
        cristalSetup(logLevel, conf, clc)
    }

    public void inMemorySetup(int logLevel = defaultLogLevel) {
        cristalSetup(logLevel, 'src/test/conf/testServer.conf', 'src/test/conf/testInMemory.clc')
        //FieldUtils.writeDeclaredStaticField(Gateway.class, "mLookupManager", Gateway.getLookup(), true)
    }

    public void inMemoryServer(String conf, String clc, int logLevel, Properties testProps = null, boolean skipBootstrap = false) {
        serverSetup(logLevel, conf, clc, testProps, skipBootstrap)
    }

    public void inMemoryServer(int logLevel = defaultLogLevel, Properties testProps = null, boolean skipBootstrap = false) {
        serverSetup(logLevel, 'src/test/conf/testServer.conf', 'src/test/conf/testInMemory.clc', testProps, skipBootstrap)
        //Thread.sleep(2000)
    }

    public Authenticator serverSetup(int logLevel, String config, String connect, Properties testProps = null, boolean skipBootstrap = false) {
        Authenticator auth = cristalSetup(logLevel, config, connect, testProps)
        Logger.initConsole("ItemServer");

        Gateway.startServer()

        if (!skipBootstrap) {
            Gateway.runBoostrap();
            waitBootstrapThread()
        }

        return auth
    }

    public Authenticator cristalSetup(int logLevel, String config, String connect, Properties testProps = null) {
        cristalInit(logLevel, config, connect, testProps)
        return Gateway.connect()
    }

    public void cristalInit(int logLevel, String config, String connect, Properties testProps = null) {
        loggerSetup(logLevel)
        Gateway.init(AbstractMain.readPropertyFiles(config, connect, testProps))
    }

    public void cristalCleanup() {
        /*
        def ORB = null

        try { ORB = Gateway.getORB() }
        catch(any) { ORB = null }

        if(ORB && ORB instanceof com.sun.corba.se.impl.orb.ORBImpl) {
            Logger.msg("Forcing Sun ORB port closure");
            try {
                com.sun.corba.se.spi.transport.CorbaTransportManager mgr = ((com.sun.corba.se.impl.orb.ORBImpl)ORB).getCorbaTransportManager();
                for (Object accept: mgr.getAcceptors()) {
                    ((com.sun.corba.se.pept.transport.Acceptor) accept).close(); 
                }
            }
            catch(Throwable t) {
                Logger.error(t)
//                System.err.println("Error closing ORB!")
//                t.printStackTrace()
            }
        }
        */
        Gateway.close()
    }
}
