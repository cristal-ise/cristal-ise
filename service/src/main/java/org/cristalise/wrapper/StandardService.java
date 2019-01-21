package org.cristalise.wrapper;
/**************************************************************************
 * StandardServer
 *
 * $Revision: 1.47 $
 * $Date: 2005/04/28 13:49:43 $
 *
 * Copyright (C) 2001 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/


import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.ShutdownHandler;
import org.cristalise.kernel.process.StandardServer;
import org.cristalise.kernel.utils.Logger;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;


/**************************************************************************
 * Base class for all servers i.e. c2k processes that serve Entities
 *
 * @author $Author: abranson $ $Date: 2005/04/28 13:49:43 $
 * @version $Revision: 1.47 $
 **************************************************************************/
public class StandardService extends StandardServer implements WrapperListener
{
    protected static StandardService server;

   /**************************************************************************
    * Sets up and runs and item server
    **************************************************************************/
    @Override
	public Integer start(String[] args)
    {
        try
        {
            //initialise everything
            standardInitialisation( args );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            Logger.die("Startup failed");
        }
        return null;
    }

    public static void main(String[] args) {
        AbstractMain.isServer = true;
        server = new StandardService();
        setShutdownHandler(new ShutdownHandler() {
			@Override
			public void shutdown(int errCode, boolean isServer) {
				WrapperManager.stop(0);
			}
		});
        WrapperManager.start( server, args );
    }

    /**
     *
     */
    @Override
	public void controlEvent(int event) {
        if (WrapperManager.isControlledByNativeWrapper()) {
            // The Wrapper will take care of this event
        } else {
            // We are not being controlled by the Wrapper, so
            //  handle the event ourselves.
            if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) ||
                (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) ||
                (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)){
                WrapperManager.stop(0);
            }
        }

    }

    /**************************************************************************
     * Closes all listeners, quits the VM.
     * This method should be called to kill the server process
     * e.g. from the NT service wrapper
     **************************************************************************/
    @Override
	public int stop(int arg0) {
    	WrapperManager.signalStopping(10000);
        try
        {
        	Gateway.close();
        }
        catch( Exception ex )
        {
            Logger.error(ex);
            return 1;
        }

        Logger.msg("StandardServer::shutdown - complete. ");
        return 0;
    }

}
