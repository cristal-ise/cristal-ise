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
import java.io.StringWriter;
import java.io.Writer;
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
import org.json.JSONArray;
import org.json.JSONObject;
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
        this("", schema, true);
    }

    public OutcomeBuilder(Schema schema, boolean initInstance) throws OutcomeBuilderException {
        this("", schema, initInstance);
    }

    public OutcomeBuilder(String root, Schema schema) throws OutcomeBuilderException {
        this(root, schema, true);
    }

    public OutcomeBuilder(String root, Schema schema, boolean initOutcome) throws OutcomeBuilderException {
        try {
            if (initOutcome) {
                Document document = Outcome.parse((InputSource)null);
                initialise(schema.getSom(), document, root);
                document.appendChild( modelRoot.initNew(document) );
                outcome = new Outcome(-1, document, schema);
            }
            else {
                initialise(schema.getSom(), null, root);
            }
        }
        catch (SAXException | IOException e) {
            Logger.error(e);
            throw new InvalidSchemaException(e.getMessage());
        }
    }

    public OutcomeBuilder(Schema schema, String xml) throws OutcomeBuilderException, InvalidDataException {
        this("", schema, xml);
    }

    public OutcomeBuilder(String root, Schema schema, Outcome outcome) throws OutcomeBuilderException {
        this.outcome = outcome;
        initialise(schema.getSom(), outcome.getDOM(), root);
        addInstance(outcome);
    }

    public OutcomeBuilder(String root, Schema schema, String xml) throws OutcomeBuilderException, InvalidDataException {
        this(root, schema, new Outcome(xml, schema));
    }

    public void initialise(org.exolab.castor.xml.schema.Schema som, Document document, String selectedRoot) throws OutcomeBuilderException {
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

        if (rootElementDecl == null) throw new InvalidSchemaException("No root element defined");

        Logger.msg(5, "OutcomeBuilder.initialise() - selected root:" + rootElementDecl.getName());

        if (rootElementDecl.getType().isSimpleType() || ((ComplexType) rootElementDecl.getType()).isSimpleContent()) {
            //modelRoot = new Field(rootElementDecl); //Simpletype could work later
            throw new InvalidSchemaException("Root element '"+rootElementDecl.getName()+"' shall not be simple type");
        }
        else {
            modelRoot = new DataRecord(rootElementDecl);
        }

        Logger.msg(5, "OutcomeBuilder.initialise() - DONE");
    }

    public void addInstance(Outcome outcome) throws OutcomeBuilderException {
        modelRoot.addInstance(outcome.getDOM().getDocumentElement(), outcome.getDOM());
    }

    //convert Map<String, Object> to Map<String, String>
    //json.toMap().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    public void addJsonInstance(JSONObject json) throws OutcomeBuilderException {
        String[] keys = json.keySet().toArray(new String[0]);

        if (keys.length != 1) throw new InvalidOutcomeException("Outcome must have a single root (length = " + keys.length + ")");

        modelRoot.addJsonInstance(outcome.getDOM().getDocumentElement(), keys[0], json.getJSONObject(keys[0]));
    }

    public void addRecord(String path, Map<String, String> record) throws OutcomeBuilderException {
        Logger.msg(5,"OutcomeBuilder.addRecord() - path:'"+path+"'");

        String[] names = StringUtils.split(path, "/");

        if (!modelRoot.getName().equals(names[0])) {
            throw new StructuralException("path does not start with rootElement: '"+path+"' ?~ '"+modelRoot.getName()+"'");
        }

        Element newElement = null;

        if(names.length == 1) {
            //updating the root, do nothing here, check setRecord() calls bellow
        }
        else if(names.length == 2) {
            newElement = modelRoot.createChildElement(outcome.getDOM(), names[1]);
        }
        else {
            String recordName = names[names.length-1];

            //Remove the first and the last entry
            OutcomeStructure modelElement = modelRoot.find(Arrays.copyOfRange(names, 1, names.length-1));

            if (modelElement == null) throw new StructuralException("Invalid path:'"+path+"'");

            newElement = modelElement.createChildElement(outcome.getDOM(), recordName);
        }

        try {
            if (newElement == null) outcome.setRecord(record);
            else                    outcome.setRecord(newElement, record);
        }
        catch (InvalidDataException e) {
            Logger.error(e);
            throw new StructuralException(e);
        }
    }

    public String getXml() throws InvalidDataException {
        outcome.validateAndCheck();
        return outcome.getData();
    }

    public Outcome getOutcome() throws InvalidDataException {
        return getOutcome(true);
    }

    public Outcome getOutcome(boolean check) throws InvalidDataException {
        if (check) outcome.validateAndCheck();
        return outcome;
    }

    public void putField(String name, String data) throws InvalidDataException {
        outcome.setField(name, data);
    }

    public String generateNgDynamicForms(Map<String, Object> inputs) {
        String json = generateNgDynamicFormsJson(inputs).toString(2);

        Logger.msg(5, "OutcomeBuilder.generateNgDynamicForms() - json:%s", json);

        return json;
    }

    public String generateNgDynamicForms() {
        return generateNgDynamicForms(null);
    }

    public JSONArray generateNgDynamicFormsJson() {
        return generateNgDynamicFormsJson(null);
    }

    public JSONArray generateNgDynamicFormsJson(Map<String, Object> inputs) {
        JSONArray array = new JSONArray();
        array.put(modelRoot.generateNgDynamicForms(inputs));
        return array;
    }

    public String exportViewTemplate() {
        Writer template = new StringWriter();

        try {
            modelRoot.exportViewTemplate(template);
        }
        catch (IOException e) {
            Logger.error(e);
        }

        return template.toString();
    }
}
