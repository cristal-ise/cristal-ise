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
package org.cristalise.gui.tabs.outcome.form;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cristalise.kernel.persistency.outcome.OutcomeValidator;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;



class OutcomeEditor extends JFrame implements ActionListener {

    boolean readOnly = false;
    File schemaFile = null;
    File instanceFile = null;
    JFileChooser chooser;
    OutcomePanel outcome;
    OutcomeValidator thisValid;

    public OutcomeEditor(File schema, File instance, boolean readOnly) {
        URL schemaURL = null;
        URL instanceURL = null;
        schemaFile = schema;
        instanceFile = instance;
        this.readOnly = readOnly;

        try {
            chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(new File(".").getCanonicalPath()));
        } catch (IOException e) {
           System.out.println("Could not initialise file dialog");
           System.exit(0);
        }


        this.setTitle("Outcome Editor");
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        GridBagLayout gridbag = new GridBagLayout();
        getContentPane().setLayout(gridbag);

        // select files if url is empty

        if (schemaFile == null) { // prompt for schema
            schemaFile = getFile("Choose Schema File", "xsd");
            if (schemaFile == null) {
                System.out.println("Cannot function without a schema");
                System.exit(1);
            }
        }

        try {
            schemaURL = schemaFile.toURI().toURL();
        } catch (Exception e) {
            System.out.println("Invalid schema URL");
            System.exit(1);
            }

        if (instanceFile == null) { // prompt for schema
            instanceFile = getFile("Choose Instance File", "xml");
        }

        try {
            instanceURL = instanceFile.toURI().toURL();
        } catch (Exception e) { }

        try {
            if (instanceFile != null && instanceFile.exists())
                outcome = new OutcomePanel(schemaURL, instanceURL, readOnly);
            else
                outcome = new OutcomePanel(schemaURL, readOnly);

            Schema thisSchema = new Schema(schemaURL.getFile(), -1, FileStringUtility.url2String(schemaURL));
            thisValid = OutcomeValidator.getValidator(thisSchema);

        } catch (Exception e) { e.printStackTrace(); System.exit(0);}


        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0; c.weighty = 1.0;
        c.gridwidth = 2; c.ipadx = 5; c.ipady = 5;
        gridbag.setConstraints(outcome, c);
        this.getContentPane().add(outcome);

        JButton saveButton = new JButton("Save");
        saveButton.setActionCommand("save");
        saveButton.addActionListener(this);
        c.gridy++; c.weighty = 0; c.gridwidth = 1;
        gridbag.setConstraints(saveButton, c);
        this.getContentPane().add(saveButton);
        if (readOnly) saveButton.setEnabled(false);

        JButton saveAsButton = new JButton("Save As");
        saveAsButton.setActionCommand("saveas");
        saveAsButton.addActionListener(this);
        c.gridx++; c.weighty = 0;
        gridbag.setConstraints(saveAsButton, c);
        this.getContentPane().add(saveAsButton);
        if (readOnly) saveAsButton.setEnabled(false);
        System.out.println("Building Outcome Panel. Please wait . . .");
        outcome.run();
        pack();
        setVisible(true);
        super.toFront();

    }

    public File getFile(String title, String fileType) {
        File targetFile = null;
        chooser.setFileFilter(new SimpleFilter(fileType));
        chooser.setDialogTitle(title);
        int returnVal = chooser.showDialog(this, "Select");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
                 targetFile = chooser.getSelectedFile();
             }
        try {
           System.out.println(fileType+"="+targetFile.toURI().toURL());
        } catch (Exception ex) { }
        return targetFile;
    }

    public static void usage() {
        System.out.println("-schema file:///schema.xsd");
        System.out.println("-inst file:///instance.xml");
        System.out.println("Leave one out to get a file open box.");
        System.exit(0);
    }
    public static void main( String[] argv ) {
        Logger.addLogStream(System.out, 6);
        File instance = null;
        File schema = null;
        boolean readOnly = false;
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equals("-schema"))
                schema = new File(argv[++i]);
            if (argv[i].equals("-inst"))
                instance = new File(argv[++i]);
            if (argv[i].equals("-readOnly"))
                readOnly = true;
            if (argv[i].equals("-help") || argv[i].equals("-h"))
                usage();
        }
        new OutcomeEditor(schema, instance, readOnly);
    }

    @Override
	public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().indexOf("save") == 0) {
            String output;
            output = outcome.getOutcome();

            String errors = thisValid.validate(output);
            if (errors != null && errors.length() > 0) {
                int choice = JOptionPane.showConfirmDialog(null, errors+"\n\nSave anyway?", "Errors validating document", JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION)
                    return;
            }

            if (instanceFile == null || e.getActionCommand().equals("saveas")) {
                instanceFile = getFile("Choose Instance File", "xml");
                if (instanceFile == null) {
                    System.out.println(output);
                    return;
                }
            }
            try {
                FileOutputStream targetStream = new FileOutputStream(instanceFile);
                targetStream.write(output.getBytes());
                targetStream.close();
            } catch (Exception ex) {ex.printStackTrace();}
        }
    }

  private class SimpleFilter extends javax.swing.filechooser.FileFilter {
    String extension;

    public SimpleFilter(String extension) {
        super();
        this.extension = extension;
    }

    @Override
	public String getDescription() {
        return extension.toUpperCase()+" Files";
    }

    @Override
	public boolean accept(File f) {
        if ((f.isFile() && f.getName().endsWith(extension.toLowerCase())) || f.isDirectory()) {
            return true;
        }
        return false;
        }
    }
}
