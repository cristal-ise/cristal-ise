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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 */
public class OutcomeBuilder {

    OutcomeStructure modelRoot;
    Outcome          outcome;

    public OutcomeBuilder(Schema schema) throws OutcomeBuilderException {
        this("", schema);
    }

    public OutcomeBuilder(String root, Schema schema) throws OutcomeBuilderException {
        try {
            Document document = Outcome.parse((InputSource)null);
            initialise(schema.getSom(), document, root);
            document.appendChild( modelRoot.initNew(document) );
            outcome = new Outcome(-1, document, schema);
        }
        catch (SAXException | IOException e) {
            Logger.error(e);
            throw new InvalidSchemaException(e.getMessage());
        }
    }

    public OutcomeBuilder(Schema schema, String xml) throws OutcomeBuilderException {
        this("", schema, xml);
    }

    public OutcomeBuilder(String root, Schema schema, String xml) throws OutcomeBuilderException {
        try {
            outcome = new Outcome(xml, schema);
            initialise(schema.getSom(), outcome.getDOM(), root);
            modelRoot.addInstance(outcome.getDOM().getDocumentElement(), outcome.getDOM());
        }
        catch (InvalidDataException | OutcomeBuilderException e) {
            Logger.error(e);
            throw new InvalidOutcomeException();
        }
    }

    public void initialise(org.exolab.castor.xml.schema.Schema som, Document document, String selectedRoot) throws OutcomeBuilderException {
        Logger.msg(5, "Initialising...");

        if (som == null) throw new InvalidSchemaException("No valid schema was supplied.");

        // find the root element declaration in the schema - may need to look for annotation??
        ElementDecl rootElementDecl = null;
        Element docElement = (document == null) ? null : document.getDocumentElement();

        HashMap<String, ElementDecl> foundRoots = new HashMap<String, ElementDecl>();
        for (ElementDecl elementDecl : som.getElementDecls()) foundRoots.put(elementDecl.getName(), elementDecl);

        if (foundRoots.size() == 0) throw new InvalidSchemaException("No root elements defined");

        if (StringUtils.isNotBlank(selectedRoot)) rootElementDecl = foundRoots.get(selectedRoot);
        else if (foundRoots.size() == 1)          rootElementDecl = foundRoots.values().iterator().next();
        else if (docElement != null)              rootElementDecl = foundRoots.get(docElement.getTagName());

        if (rootElementDecl == null) throw new InvalidSchemaException("No root elements defined");

        if (rootElementDecl.getType().isSimpleType() || ((ComplexType) rootElementDecl.getType()).isSimpleContent())
            throw new InvalidSchemaException("Root element '"+rootElementDecl.getName()+"' shall not be simple type name");

        modelRoot = new DataRecord(rootElementDecl, false);

        Logger.msg(5, "Finished structure!");
    }

    public void addRecord(String path, Map<String, String> record) throws OutcomeBuilderException {
        Logger.msg(5,"Add record to '"+path+"'");

        String[] names = StringUtils.split(path, "/");

        if (!modelRoot.getName().equals(names[0])) {
            throw new StructuralException("path does not start with rootElement: '"+path+"' ?~ '"+modelRoot.getName()+"'");
        }

        if(names.length == 1) {
            modelRoot.putFields(record);
        }
        else if(names.length == 2) {
            modelRoot.addRecord(outcome.getDOM(), names[1], record);
        }
        else {
            String recordName = names[names.length-1];

            //Remove the first and the last entry
            OutcomeStructure modelElement = modelRoot.find(Arrays.copyOfRange(names, 1, names.length-2));

            if (modelElement == null) throw new StructuralException("Invalid path:'"+path+"'");

            modelElement.addRecord(outcome.getDOM(), recordName, record);
        }
    }

    public String getXml() throws InvalidDataException {
        modelRoot.validateStructure();
        return outcome.getData();
    }

    public void putField(String name, String data) throws InvalidDataException {
        outcome.setField(name, data);
    }
}
