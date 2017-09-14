/**
 * This file is part of the CRISTAL-iSE XPath Outcome Initiator module.
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
package org.cristalise.kernel.persistency.outcomebuilder;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Schema;
import org.exolab.castor.xml.schema.reader.SchemaReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *  will load the outcome as instructed by other bits of the gui
 *  provides the 'save' button and creates the trees of objects to feed to the outcome form
 */
public class OutcomeBuilder {

    Schema           schemaSOM;
    Document         outcomeDOM;
    OutcomeStructure documentRoot;
    DocumentBuilder  parser;
    boolean          readOnly;
    boolean          useForm    = true;
    boolean          unsaved    = false;

    protected HashMap<String, Class<?>> specialEditFields = new HashMap<String, Class<?>>();

    public OutcomeBuilder() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);

        try {
            parser = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            Logger.error(e);
        }
    }

    public OutcomeBuilder(boolean readOnly) {
        this();
        setReadOnly(readOnly);
    }

    public OutcomeBuilder(String schema, boolean readOnly) throws OutcomeException, InvalidSchemaException {
        this(readOnly);
        this.setDescription(schema);
    }

    public OutcomeBuilder(String schema, String outcome, boolean readOnly) throws OutcomeException, InvalidOutcomeException, InvalidSchemaException {
        this(readOnly);
        this.setDescription(schema);
        this.setOutcome(outcome);
    }

    // Parse from URLS
    public void setOutcome(URL outcomeURL) throws InvalidOutcomeException {
        try {
            setOutcome(new InputSource(outcomeURL.openStream()));
        }
        catch (IOException ex) {
            throw new InvalidOutcomeException("Error creating instance DOM tree: " + ex);
        }
    }

    public void setDescription(URL schemaURL) throws InvalidSchemaException {
        Logger.msg(7, "OutcomeBulder.setDescription() - schemaURL:" + schemaURL.toString());
        try {
            setDescription(new InputSource(schemaURL.openStream()));
        }
        catch (IOException ex) {
            throw new InvalidSchemaException("Error creating exolab schema object: " + ex);
        }

    }

    public OutcomeBuilder(URL schemaURL, boolean readOnly) throws OutcomeException, InvalidSchemaException {
        this(readOnly);
        this.setDescription(schemaURL);
    }

    public OutcomeBuilder(URL schemaURL, URL outcomeURL, boolean readOnly) throws OutcomeException, InvalidSchemaException, InvalidOutcomeException {
        this(readOnly);
        this.setDescription(schemaURL);
        this.setOutcome(outcomeURL);
    }

    // Parse from Strings
    public void setOutcome(String outcome) throws InvalidOutcomeException {

        try {
            setOutcome(new InputSource(new StringReader(outcome)));
        }
        catch (IOException ex) {
            throw new InvalidOutcomeException("Error creating instance DOM tree: " + ex);
        }
    }

    public void setDescription(String schema) throws InvalidSchemaException {
        if (schema == null) throw new InvalidSchemaException("Null schema supplied");

        try {
            setDescription(new InputSource(new StringReader(schema)));
        }
        catch (Exception ex) {
            Logger.error(ex);
        }
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setDescription(InputSource schemaSource) throws InvalidSchemaException, IOException {

        SchemaReader mySchemaReader = new SchemaReader(schemaSource);
        this.schemaSOM = mySchemaReader.read();
    }

    public void setOutcome(InputSource outcomeSource) throws InvalidOutcomeException, IOException {
        try {
            outcomeDOM = parser.parse(outcomeSource);
        }
        catch (SAXException ex) {
            throw new InvalidOutcomeException("Sax error parsing Outcome " + ex);
        }
    }

    public void initialise() throws OutcomeException, InvalidSchemaException {
        Element docElement;
        Logger.msg(5, "Initialising..");

        if (schemaSOM == null) throw new InvalidSchemaException("A valid schema has not been supplied.");

        // find the root element declaration in the schema - may need to look for annotation??
        ElementDecl rootElementDecl = null;
        docElement = (outcomeDOM == null) ? null : outcomeDOM.getDocumentElement();

        HashMap<String, ElementDecl> foundRoots = new HashMap<String, ElementDecl>();
        for (ElementDecl elementDecl : schemaSOM.getElementDecls()) foundRoots.put(elementDecl.getName(), elementDecl);

        if (foundRoots.size() == 0) throw new InvalidSchemaException("No root elements defined");

        if (foundRoots.size() == 1)  rootElementDecl = foundRoots.values().iterator().next();
        else if (docElement != null) rootElementDecl = foundRoots.get(docElement.getTagName());
        else { // choose root
            //FIXME Choose the root element
            //String[] rootArr = foundRoots.keySet().toArray(new String[0]);
            String choice = "Choose the root element";
            rootElementDecl = foundRoots.get(choice);
        }

        if (rootElementDecl == null) throw new InvalidSchemaException("No root elements defined");

        if (rootElementDecl.getType().isSimpleType() || ((ComplexType) rootElementDecl.getType()).isSimpleContent())
            documentRoot = new Field(rootElementDecl, readOnly, specialEditFields);
        else
            documentRoot = new DataRecord(rootElementDecl, readOnly, false, specialEditFields);

        Logger.msg(5, "Finished structure. Populating...");

        //FIXME: refactor instantiating a new XML Document
        //if (docElement == null) {
        //    outcomeDOM = parser.newDocument();
        //    docElement = documentRoot.initNew(outcomeDOM);
        //    outcomeDOM.appendChild(docElement);
        //}
        //else
        //    documentRoot.addInstance(docElement, outcomeDOM);
    }

    public String getOutcome() {
        if (useForm) {
            documentRoot.validateStructure();
            try {
                return Outcome.serialize(outcomeDOM, false);
            }
            catch (InvalidDataException e) {}
        }
        else {
            //return basicView.getText();
        }
        return "";
    }
}
