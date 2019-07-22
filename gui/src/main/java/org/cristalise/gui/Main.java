/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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


import java.io.File;
import java.util.Arrays;

import javax.swing.JFileChooser;

import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.StandardClient;
import org.cristalise.kernel.utils.Logger;


/**
 *
 */
public class Main extends StandardClient
{
    static public void main(String[] args)
    {
        try
        {
            if (args[args.length-1].equals("-connect")) { // prompt for connect file
                    JFileChooser clcChooser = new JFileChooser();
                    clcChooser.setDialogTitle("Please choose a CRISTAL connect file.");
                    clcChooser.addChoosableFileFilter(
                        new javax.swing.filechooser.FileFilter() {
                            @Override
                            public String getDescription() {
                                return "CRISTAL Connect Files";
                            }
                            @Override
                            public boolean accept(File f) {
                                if (f.isDirectory() || (f.isFile() && f.getName().endsWith(".clc"))) {
                                    return true;
                                }
                                return false;
                            }
                        });
                    int returnVal = clcChooser.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File targetFile = clcChooser.getSelectedFile();
                        args = Arrays.copyOf(args, args.length+1);
                        args[args.length-1] = targetFile.getCanonicalPath();
                    }
                        
            }
            Gateway.init(readC2KArgs(args));
            Logger.initConsole("GUI");
            Gateway.connect();
            MainFrame client = new MainFrame();
            client.showLogin();

        }
        catch( Exception ex )
        {
            ex.printStackTrace();

            try
            {
                Gateway.close();
            }
            catch(Exception ex1)
            {
                ex1.printStackTrace();
            }
        }
    }
}
