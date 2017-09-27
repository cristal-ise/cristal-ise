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
 *
 */
public class OutcomeBuilder {

    Schema           schemaSOM;
    Document         outcomeDOM;
    OutcomeStructure documentRoot;
    DocumentBuilder  parser;

    boolean          unsaved      = false;
    String           selectedRoot = "Storage";

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

    public OutcomeBuilder(String schema) throws OutcomeException, InvalidSchemaException {
        this();
        this.setSchema(schema);
    }

    public OutcomeBuilder(String schema, String outcome) throws OutcomeException, InvalidOutcomeException, InvalidSchemaException {
        this();
        this.setSchema(schema);
        this.setOutcome(outcome);
    }

    public OutcomeBuilder(URL schemaURL) throws OutcomeException, InvalidSchemaException {
        this();
        this.setSchema(schemaURL);
    }

    public OutcomeBuilder(URL schemaURL, URL outcomeURL) throws OutcomeException, InvalidSchemaException, InvalidOutcomeException {
        this();
        this.setSchema(schemaURL);
        this.setOutcome(outcomeURL);
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

    // Parse from Strings
    public void setOutcome(String outcome) throws InvalidOutcomeException {
        try {
            setOutcome(new InputSource(new StringReader(outcome)));
        }
        catch (IOException ex) {
            throw new InvalidOutcomeException("Error creating instance DOM tree: " + ex);
        }
    }

    public void setSchema(URL schemaURL) throws InvalidSchemaException {
        Logger.msg(7, "OutcomeBulder.setSchema() - schemaURL:" + schemaURL.toString());
        try {
            setSchema(new InputSource(schemaURL.openStream()));
        }
        catch (IOException ex) {
            throw new InvalidSchemaException("Error creating exolab schema object: " + ex);
        }
    }

    public void setSchema(String schema) throws InvalidSchemaException {
        if (schema == null) throw new InvalidSchemaException("Null schema supplied");

        try {
            setSchema(new InputSource(new StringReader(schema)));
        }
        catch (Exception ex) {
            Logger.error(ex);
        }
    }

    public void setSchema(InputSource schemaSource) throws InvalidSchemaException, IOException {

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
        Logger.msg(5, "Initialising...");

        if (schemaSOM == null) throw new InvalidSchemaException("A valid schema has not been supplied.");

        // find the root element declaration in the schema - may need to look for annotation??
        ElementDecl rootElementDecl = null;
        docElement = (outcomeDOM == null) ? null : outcomeDOM.getDocumentElement();

        HashMap<String, ElementDecl> foundRoots = new HashMap<String, ElementDecl>();
        for (ElementDecl elementDecl : schemaSOM.getElementDecls()) foundRoots.put(elementDecl.getName(), elementDecl);

        if (foundRoots.size() == 0) throw new InvalidSchemaException("No root elements defined");

        if (foundRoots.size() == 1)  rootElementDecl = foundRoots.values().iterator().next();
        else if (docElement != null) rootElementDecl = foundRoots.get(docElement.getTagName());
        else                         rootElementDecl = foundRoots.get(selectedRoot);  //choose root

        if (rootElementDecl == null) throw new InvalidSchemaException("No root elements defined");

        if (rootElementDecl.getType().isSimpleType() || ((ComplexType) rootElementDecl.getType()).isSimpleContent())
            documentRoot = new Field(rootElementDecl, specialEditFields);
        else
            documentRoot = new DataRecord(rootElementDecl, false, specialEditFields);

        Logger.msg(5, "Finished structure!");
    }

    public void createNewOutcome() {
        outcomeDOM = parser.newDocument();
        outcomeDOM.appendChild( documentRoot.initNew(outcomeDOM) );
    }

    public void addInstance() throws OutcomeException {
        Element docElement = (outcomeDOM == null) ? null : outcomeDOM.getDocumentElement();
        documentRoot.addInstance(docElement, outcomeDOM);
    }

    public String getOutcome() throws InvalidDataException {
        documentRoot.validateStructure();
        return Outcome.serialize(outcomeDOM, false);
    }

    public String getSelectedRoot() {
        return selectedRoot;
    }

    public void setSelectedRoot(String selectedRoot) {
        this.selectedRoot = selectedRoot;

        outcomeDOM = null;
    }
}
