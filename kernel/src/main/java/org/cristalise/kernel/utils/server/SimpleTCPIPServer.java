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
package org.cristalise.kernel.utils.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SimpleTCPIPServer implements Runnable {

    int                      port            = 0;
    int                      maxConn         = 10;
    Thread                   listener        = null;
    Class<?>                 handlerClass    = null;
    ServerSocket             serverSocket    = null;
    boolean                  keepListening   = true;
    ArrayList<SocketHandler> currentHandlers = new ArrayList<SocketHandler>();
    static short             numberOfServers = 0;

    public SimpleTCPIPServer(int port, Class<?> handlerClass, int maxConnections) {
        this.port         = port;
        this.handlerClass = handlerClass;
        this.maxConn      = maxConnections;
        numberOfServers++;
    }

    public void startListening() {
        if(listener != null) return;
        keepListening = true;

        listener = new Thread(this);
        listener.start();
    }

    public void stopListening() {
        log.info("stopListening() - Closing server for " + handlerClass.getName() +" on port "+ port);

        keepListening = false;
        for (SocketHandler thisHandler : currentHandlers) {
            thisHandler.shutdown();
        }
        try {
            if (serverSocket!=null) serverSocket.close();
        }
        catch (IOException e) { }
    }

    @Override
	public void run() {
        Thread.currentThread().setName("TCP/IP Server for class "+handlerClass.getName());
        Socket connectionSocket = null;

        try {
            serverSocket = new ServerSocket(port);
            if (port == 0) port = serverSocket.getLocalPort();

            log.info("run() - Created server for " + handlerClass.getName()+" on port "+port);

            serverSocket.setSoTimeout(500);
            SocketHandler freeHandler = null;

            while(keepListening) {
                if (freeHandler == null || freeHandler.isBusy()) {
                    ListIterator<SocketHandler> i = currentHandlers.listIterator();
                    try {
                        do {
                            freeHandler = i.next();
                        }
                        while (freeHandler.isBusy());
                    }
                    catch (NoSuchElementException e) {
                        // create new one
                        if (currentHandlers.size() < maxConn) {
                            freeHandler = (SocketHandler)handlerClass.getDeclaredConstructor().newInstance();
                            currentHandlers.add(freeHandler);
                        }
                        else { // max handlers are created. wait for a while, then look again
                            log.warn("No free handlers left for "+handlerClass.getName()+" on port "+ port + "! Sleeping 2s.");
                            Thread.sleep(2000);
                            continue;
                        }
                    }
                }

                try {
                    connectionSocket = serverSocket.accept();
                    if (keepListening) {
                        log.info("Connection to "+freeHandler.getName()+" from "+ connectionSocket.getInetAddress());

                        freeHandler.setSocket(connectionSocket);
                        new Thread(freeHandler).start();
                    }
                } 
                catch (SocketTimeoutException ex1) { }// timeout just to check if we've been told to die
                catch (SocketException ex1)        { } // we were closed during shutdown
            }
            serverSocket.close();
            log.info("Server closed for " + handlerClass.getName() +" on port "+ port);
        }
        catch(Exception ex) {
            log.error("run(): Fatal Error. Listener for '"+handlerClass.getName()+"' will stop.", ex);
        }
        listener = null;
        log.info("Servers still running: "+--numberOfServers);
    }

    public int getPort() {
        return port;
    }
}
