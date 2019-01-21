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
package org.cristalise.gui.tabs.outcome.form;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cristalise.gui.tabs.outcome.InvalidOutcomeException;
import org.cristalise.gui.tabs.outcome.InvalidSchemaException;
import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.gui.tabs.outcome.OutcomeHandler;
import org.cristalise.gui.tabs.outcome.OutcomeNotInitialisedException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Schema;
import org.exolab.castor.xml.schema.reader.SchemaReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


// will load the outcome as instructed by other bits of the gui
// provides the 'save' button and creates the trees of objects to feed to the outcome form

public class OutcomePanel extends JPanel implements OutcomeHandler
{

    Schema schemaSOM;
    Document outcomeDOM;
    OutcomeStructure documentRoot;
    DocumentBuilder parser;
    boolean readOnly;
    boolean useForm = true;
    boolean panelBuilt = false;
    boolean unsaved = false;
    JScrollPane scrollpane = new JScrollPane();
    protected HashMap<String, Class<?>> specialEditFields = new HashMap<String, Class<?>>();
    JTextArea basicView;
    
    public OutcomePanel()
    {
        GridBagLayout gridbag = new java.awt.GridBagLayout();
        setLayout(gridbag);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);
        try
        {
            parser = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            e.printStackTrace();
        }

        // Set up panel

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.ipadx = 5;
        c.ipady = 5;

        gridbag.setConstraints(scrollpane, c);
        this.add(scrollpane);
    }

    public OutcomePanel(boolean readOnly)
    {
        this();
        setReadOnly(readOnly);
    }

    public OutcomePanel(String schema, boolean readOnly) throws OutcomeException
    {
        this(readOnly);
        this.setDescription(schema);
    }

    public OutcomePanel(String schema, String outcome, boolean readOnly) throws OutcomeException
    {
        this(readOnly);
        this.setDescription(schema);
        this.setOutcome(outcome);
    }

    // Parse from URLS
    public void setOutcome(URL outcomeURL) throws InvalidOutcomeException
    {

        try
        {
            setOutcome(new InputSource(outcomeURL.openStream()));
        }
        catch (IOException ex)
        {
            throw new InvalidOutcomeException("Error creating instance DOM tree: " + ex);
        }
    }

    public void setDescription(URL schemaURL) throws InvalidSchemaException
    {
        Logger.msg(7, "OutcomePanel.setDescription() - schemaURL:" + schemaURL.toString());
        try
        {
            setDescription(new InputSource(schemaURL.openStream()));
        }
        catch (IOException ex)
        {
            throw new InvalidSchemaException("Error creating exolab schema object: " + ex);
        }

    }

    public OutcomePanel(URL schemaURL, boolean readOnly) throws OutcomeException
    {
        this(readOnly);
        this.setDescription(schemaURL);
    }

    public OutcomePanel(URL schemaURL, URL outcomeURL, boolean readOnly) throws OutcomeException
    {
        this(readOnly);
        this.setDescription(schemaURL);
        this.setOutcome(outcomeURL);
    }

    // Parse from Strings
    @Override
	public void setOutcome(String outcome) throws InvalidOutcomeException
    {

        try
        {
            setOutcome(new InputSource(new StringReader(outcome)));
        }
        catch (IOException ex)
        {
            throw new InvalidOutcomeException("Error creating instance DOM tree: " + ex);
        }
    }

    @Override
	public void setDescription(String schema) throws InvalidSchemaException
    {
        if (schema == null)
            throw new InvalidSchemaException("Null schema supplied");
        try
        {
            setDescription(new InputSource(new StringReader(schema)));
        }
        catch (Exception ex)
        {
            Logger.error(ex);
        }

    }

    @Override
	public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public void setDescription(InputSource schemaSource) throws InvalidSchemaException, IOException
    {

        SchemaReader mySchemaReader = new SchemaReader(schemaSource);
        this.schemaSOM = mySchemaReader.read();
    }

    public void setOutcome(InputSource outcomeSource) throws InvalidOutcomeException, IOException
    {
        try
        {
            outcomeDOM = parser.parse(outcomeSource);
        }
        catch (SAXException ex)
        {
            throw new InvalidOutcomeException("Sax error parsing Outcome " + ex);
        }
    }

    @Override
	public void run()
    {
        Thread.currentThread().setName("Outcome Panel Builder");
        try
        {
            makeDisplay();
        }
        catch (Exception oe)
        {
            scrollpane.setViewportView(new JLabel("Outcome View Generation Failed: " + oe.getMessage()));
            Logger.error(oe);
        }
    }

    public void makeDisplay()
    {
        try
        {
            initPanel();
        }
        catch (OutcomeException ex)
        {
            // something went wrong
            useForm = false;
            Box textPanel = Box.createVerticalBox();
            JLabel errorMsg = new JLabel("Could not create outcome view: " + ex.getMessage());
            errorMsg.setHorizontalAlignment(SwingConstants.LEFT);
            textPanel.add(errorMsg);
            textPanel.add(Box.createVerticalGlue());
            if (outcomeDOM!=null) {
                String xml = "";

                try {
                    xml = Outcome.serialize(outcomeDOM, true);
                }
                catch (InvalidDataException e) {}

                basicView = new JTextArea(xml);
                basicView.setEnabled(!readOnly);
                textPanel.add(basicView);
            }
            scrollpane.setViewportView(textPanel);
        }
        
    }

    public void initPanel() throws OutcomeException
    {
        Element docElement;
        /*if (panelBuilt)
            return;*/
        Logger.msg(5, "Initialising Panel..");
        scrollpane.setViewportView(new JLabel("Building outcome. Please hang on two ticks . . ."));
        if (schemaSOM == null)
            throw new InvalidSchemaException("A valid schema has not been supplied.");
        // create root panel with element declaration and maybe root document element node

        //find the root element declaration in the schema - may need to look for annotation??
        ElementDecl rootElementDecl = null;
        docElement = (outcomeDOM == null) ? null : outcomeDOM.getDocumentElement();

        HashMap<String, ElementDecl> foundRoots = new HashMap<String, ElementDecl>();
        for (ElementDecl elementDecl: schemaSOM.getElementDecls())
        	foundRoots.put(elementDecl.getName(), elementDecl);
        if (foundRoots.size() == 0)
        	throw new InvalidSchemaException("No root elements defined");
        if (foundRoots.size() == 1)
        	rootElementDecl = foundRoots.values().iterator().next();
        else if (docElement != null)
        	rootElementDecl = foundRoots.get(docElement.getTagName());
        else { //choose root
        	String[] rootArr = foundRoots.keySet().toArray(new String[0]);
        	String choice = (String) JOptionPane.showInputDialog(this, "Choose the root element:",
        			"Multiple possible root elements found", JOptionPane.PLAIN_MESSAGE,
        			null, rootArr, rootArr[0]);
        	rootElementDecl = foundRoots.get(choice);
        }

        if (rootElementDecl == null)
            throw new InvalidSchemaException("No root elements defined");
        
        if (rootElementDecl.getType().isSimpleType() || ((ComplexType)rootElementDecl.getType()).isSimpleContent())
        	documentRoot = new Field(rootElementDecl, readOnly, specialEditFields);
        else
        	documentRoot = new DataRecord(rootElementDecl, readOnly, false, specialEditFields);

        Logger.msg(5, "Finished structure. Populating...");
        if (docElement == null)
        {
            outcomeDOM = parser.newDocument();
            docElement = documentRoot.initNew(outcomeDOM);
            outcomeDOM.appendChild(docElement);
        }
        else
            documentRoot.addInstance(docElement, outcomeDOM);

        // got a fully rendered Outcome! put it in the scrollpane
        // initialise container panel

        JTabbedPane outcomeTab = new JTabbedPane();
        outcomeTab.addTab(rootElementDecl.getName(), documentRoot);
        outcomeTab.setSelectedIndex(0);

        scrollpane.setViewportView(outcomeTab);
        panelBuilt = true;

        revalidate();
        doLayout();
        if (!readOnly)
            documentRoot.grabFocus();
    }

    @Override
	public JPanel getPanel() throws OutcomeNotInitialisedException
    {
        return this;
    }

    @Override
	public String getOutcome()
    {
        if (useForm)
        {
            documentRoot.validateStructure();
            try {
                return Outcome.serialize(outcomeDOM, false);
            }
            catch (InvalidDataException e) {}
            return "";
        }
        else
        {
            return basicView.getText();
        }
    }

    @Override
	public boolean isUnsaved() {
        return unsaved;
    }

    @Override
	public void saved() {
        unsaved = false;
    }

	@Override
	public void export(File targetFile) throws Exception {
		FileStringUtility.string2File(targetFile, getOutcome());
	}
}
