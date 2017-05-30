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
package org.cristalise.kernel.persistency.outcomeinit;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.apache.xmlbeans.impl.xsd2inst.SampleXmlUtil;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeInitiator;
import org.cristalise.kernel.utils.Logger;

/**
 * OutcomeInitiator implementation creating an 'empty' Outcome from XML a Schema.
 * It is based on Apache XMLBeans.
 */
public class EmptyOutcomeInitiator implements OutcomeInitiator {

    /**
     * The name of the Activity Property containing the root element name in the Schema.
     * The property can be undefined.
     */
    public static final String ROOTNAME_PROPNAME = "SchemaRootElementName";

    /**
     * Created the option required by XML Beans, 
     * 
     * @return the XmlOptions
     */
    protected static XmlOptions getXSDCompileOptions() {
        XmlOptions options = new XmlOptions();

        options.setCompileDownloadUrls();
        options.setCompileNoPvrRule();
        options.setCompileNoUpaRule();

        return options;
    }

    /**
     * Return the root element
     * 
     * @param rootName
     * @param sts SchemaTypeSystem
     * @return SchemaType
     * @throws InvalidDataException
     */
    protected static SchemaType getRootElement(String rootName, SchemaTypeSystem sts) throws InvalidDataException {
        SchemaType[] globalElems = sts.documentTypes();
        
        if (globalElems == null) throw new InvalidDataException("Schema has no global elements.");

        if(rootName == null) {
            Logger.msg(5, "EmptyOutcomeInitiator.getRootElement() - rootName is null, taking the root from Schema");

            if (globalElems.length != 1) throw new InvalidDataException("Ambiguious root: Schema has more than one global elements");

            return globalElems[0];
        }
        else {
            Logger.msg(5, "EmptyOutcomeInitiator.getRootElement() - rootName:"+rootName);

            SchemaType elem = null;
            boolean found = false;

            for (int i = 0; i < globalElems.length && !found; i++) {
                if (rootName.equals(globalElems[i].getDocumentElementName().getLocalPart())) {
                    elem = globalElems[i];
                    found = true;
                }
            }

            if (!found) {
                Logger.error("Could not find a global element with name '" + rootName + "'");
                throw new InvalidDataException("Could not find a global element with name '" + rootName + "'");
            }

            return elem;
        }
    }
    
    /**
     * 
     * @param xsd the input Schema
     * @return initialized SchemaTypeSystem instance
     * @throws InvalidDataException
     */
    protected static SchemaTypeSystem getSchemaTypeSystem(String xsd) throws InvalidDataException {
        SchemaDocument[] schemas = new SchemaDocument[1];

        try {
            schemas[0] = SchemaDocument.Factory.parse( xsd, (new XmlOptions()).setLoadLineNumbers().setLoadMessageDigest());
        }
        catch (XmlException e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }

        SchemaTypeSystem sts = null;
        @SuppressWarnings("rawtypes")
        Collection errors = new ArrayList();

        try {
            sts = XmlBeans.compileXsd(schemas, XmlBeans.getBuiltinTypeSystem(), getXSDCompileOptions());
        }
        catch (Exception e) {
            Logger.error(xsd);
            Logger.error(e);

            StringBuffer buffer = new StringBuffer();
            
            for (Object error: errors ) buffer.append(error.toString());

            Logger.error("Errors to process Schema(s) : " + buffer.toString());
            throw new InvalidDataException("Errors to process Schema(s) : " + buffer.toString());
        }

        if (sts == null) throw new InvalidDataException("No Schemas to process.");

        return sts;
    }

    /**
     * Returns the generated sample xml. This method is only needed to break the potential infinitive recursive calls of 
     * initOutcome() between this class and its subclasses.
     * 
     * @param rootName the name of the root element, can be null
     * @param xsd the XSD string
     * @return the generated sample xml
     * @throws InvalidDataException
     */
    protected String getXMLString(String rootName, String xsd) throws InvalidDataException {
        return SampleXmlUtil.createSampleForType( getRootElement(rootName, getSchemaTypeSystem(xsd)) );
    }

    /**
     * Creates an initial instance of an Outcome XML using SampleXmlUtil class of Apache XMLBeans
     */
    @Override
    public String initOutcome(Job job) throws InvalidDataException {
        try {
            return getXMLString( job.getActPropString(ROOTNAME_PROPNAME), job.getSchema().getSchemaData() );
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }
    }

    /**
     * Creates an initial instance of an Outcome using SampleXmlUtil class of Apache XMLBeans
     */
    @Override
    public Outcome initOutcomeInstance(Job job) throws InvalidDataException {
        try {
            String xml = getXMLString( job.getActPropString(ROOTNAME_PROPNAME), job.getSchema().getSchemaData() );
            return new Outcome(-1, xml, job.getSchema());
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }
    }
}
