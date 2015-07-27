/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
 *
 * $Revision: 1.10 $
 * $Date: 2005/10/05 07:39:37 $
 *
 * Copyright (C) 2003 CERN - European Organization for Nuclear Research
 * All rights reserved.
 **************************************************************************/

public class Console extends JFrame {
    JTextArea output;
    JScrollPane scroll;
    JTextField input;
    JButton sendButton;
    JButton toFileButton;
    FileWriter logFile;
    ConsoleConnection connection;
    JFileChooser scriptLoader = new JFileChooser();
    static int bufferSize = Gateway.getProperties().getInt("Console.bufferSize", 200);

    public Console(String host, int port) {
        super("Cristal Console - "+host);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        GridBagLayout gridbag = new GridBagLayout();
        getContentPane().setLayout(gridbag);
        output = new JTextArea("Type 'help' for help. . .\n");
        output.setEditable(false);
        input = new JTextField();
        setSize(400, 600);
        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
                submit();
            }
        });
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
                synchronized (output) {
                    output.setText("");
                }
            }
        });
        toFileButton = new JButton("Save");
        toFileButton.addActionListener(new ActionListener() {
            @Override
			public void actionPerformed(ActionEvent e) {
            	if (logFile == null) {
	                int returnValue = scriptLoader.showSaveDialog(null);
	                switch (returnValue)
	                {
	                    case JFileChooser.APPROVE_OPTION :
	                        try {
	                            logFile = new FileWriter(scriptLoader.getSelectedFile());
	                            print ("Starting writing log to "+scriptLoader.getSelectedFile().getAbsolutePath());
	                        } catch (Exception ex) {
	                            print(ex.getClass().getName()+": "+ex.getMessage());
	                            Logger.error(ex);
	                        }
	                        toFileButton.setText("Stop");
	                    case JFileChooser.CANCEL_OPTION :
	                    case JFileChooser.ERROR_OPTION :
	                    default :
	                }
            	}
            	else {
            		try {
            			logFile.close();
            		} catch (Exception ex) {
            			logFile = null;
            			print(ex.getClass().getName()+": "+ex.getMessage());
            		}
            		logFile = null;
            		toFileButton.setText("Save");
            	}
            }
        });


        input.addKeyListener(new EnterListener(this));

        scroll = new JScrollPane(output);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx=0; c.gridy=0;
        c.fill=GridBagConstraints.BOTH;
        c.weightx=1.0;c.weighty=1.0;
        gridbag.setConstraints(scroll, c);
        getContentPane().add(scroll);

        Box inputBox = Box.createHorizontalBox();
        inputBox.add(input);
        inputBox.add(Box.createHorizontalStrut(5));
        inputBox.add(sendButton);
        inputBox.add(clearButton);
        inputBox.add(toFileButton);
        c.gridy=1; c.fill=GridBagConstraints.HORIZONTAL;
        c.weighty=0;
        gridbag.setConstraints(inputBox, c);
        getContentPane().add(inputBox);

        try {
        	// TODO: merge module script utilities together and prepend with namespace
       		Properties utilProps = FileStringUtility.loadConfigFile( Gateway.getResource().findTextResource("ScriptUtils.conf") );
        	
            Box utilBox = Box.createHorizontalBox();
            for (Object name2 : utilProps.keySet()) {
                String name = (String) name2;
                String value = utilProps.getProperty(name);
                JButton newUtil = new JButton(name);
                newUtil.setActionCommand(value);
                newUtil.addActionListener(new ActionListener() {
                    @Override
					public void actionPerformed(ActionEvent e) {
                        processUtil(e.getActionCommand());
                    }
                });
                utilBox.add(newUtil);
                utilBox.add(Box.createHorizontalStrut(5));
            }

            c.gridy++;
            gridbag.setConstraints(utilBox, c);
            getContentPane().add(utilBox);
        } catch (Exception ex) { // no domain utils
        }


        validate();
        connection = new ConsoleConnection(host, port, this);
        new Thread(connection).start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
			public void windowClosing(java.awt.event.WindowEvent evt) {
                if (connection!=null) connection.shutdown();
                dispose();
            }
        });
    }

    @Override
	public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) input.requestFocus();
    }

    public void processUtil(String command) {
        int replace;
        String text = input.getText();
        while ((replace = command.indexOf("%s")) > -1) {
            command = command.substring(0, replace)+text+command.substring(replace+2);
        }
        connection.sendCommand(command);
    }

    public void submit() {
        connection.sendCommand(input.getText());
        input.setText("");
    }

    public void print(String line) {
        synchronized (output) {
            String currentText = output.getText()+line+"\n";
            while (output.getLineCount() > bufferSize) {
                currentText = currentText.substring(currentText.indexOf("\n")+1);
                output.setText(currentText);
            }
            output.setText(currentText);
            output.setCaretPosition(output.getText().length());
            if (logFile != null) try {
            	logFile.write(line+"\n");
            } catch (IOException ex) {
            	logFile = null;
            	print("Error writing to file.");
            }
        }
    }

    @Override
	public void disable() {
        synchronized (output) {
            output.append("Lost connection");
        }
        output.setEnabled(false);
        input.setEnabled(false);
        sendButton.setEnabled(false);
    }

    private class EnterListener extends KeyAdapter
    {
        Console parent;
        public EnterListener(Console parent) {
            this.parent = parent;
        }
        @Override
		public void keyPressed(KeyEvent e) {
            if (e.getKeyCode()==10) {
                parent.submit();
            }
        }
    };

    private class ConsoleConnection implements Runnable {
        String host; int port; Console parent; boolean keepConnected = true;
        Socket conn; PrintWriter consoleOutput; BufferedReader consoleInput;


        public ConsoleConnection(String host, int port, Console parent) {
            Thread.currentThread().setName("Console Client to "+host+":"+port);
            this.host = host;
            this.port = port;
            this.parent = parent;
        }

        @Override
		public void run() {
            connect();
            while (keepConnected) {
                try {
                    String line = consoleInput.readLine();
                    if (line == null) {
                        parent.disable();
                        keepConnected = false;
                    }
                    else
                        parent.print(line);
                } catch (InterruptedIOException ex) { // timeout - ignore
                } catch (IOException ex) { // error reading
                    parent.disable();
                    keepConnected = false;
                }
            }

            try {
                conn.close();
            } catch (IOException ex) { }
        }

        public void sendCommand(String command) {
            consoleOutput.println(command);
        }

        public void shutdown() {
            keepConnected = false;
        }

        public void connect() {
            parent.print("Connecting to "+host+":"+port);
            try {
                conn = new Socket(host, port);
                conn.setKeepAlive(true);
                conn.setSoTimeout(500);
                consoleOutput = new PrintWriter(conn.getOutputStream(), true);
                consoleInput = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } catch (Exception ex) {

            }
        }
    }
}
