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
package org.cristalise.kernel.persistency.outcome;

import static org.cristalise.kernel.SystemProperties.Outcome_Validation_useDOM;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.ElementSelectors;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * A C2KLocalObject encapsulating management of XML data. It has methods to manipulate and validate the XML,
 * and with a valid ID it can be stored in ClusterStore.
 *
 * It contains lot of utility code to read and set data in the Outcome (xml).
 */
@Accessors(prefix = "m") @Getter @Setter @Slf4j
public class Outcome implements C2KLocalObject {

    //These values are set in system properties to select more efficient xpath evaluation behaviour
    private static final String DTM_MANAGER_NAME  = "com.sun.org.apache.xml.internal.dtm.DTMManager";
    private static final String DTM_MANAGER_VALUE = "com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault";

    private static final int NONE = -1;

    /** ID is the eventID created when the Outcome is stored in History. Can be NONE = -1 */
    Integer mID;

    /** The Schema object associated with the Outcome. Can be null. */
    Schema mSchema = null;

    /** The name or UUID of the Schema item associated with the Outcome. Can be null. */
    String mSchemaName = null;

    /** The version of the Schema item associated with the Outcome. Can be null. */
    Integer mSchemaVersion;

    /** The parsed XML document */
    Document mDOM;

    /** Parser of  XML Documents */
    static DocumentBuilder parser;

    /** Use this static ThreadLocal variable for thread-safe XPath evaluation */
    private static final ThreadLocal<XPathFactory> XPATH_FACTORY = new ThreadLocal<XPathFactory>() {
        @Override
        protected XPathFactory initialValue() {
            return XPathFactory.newInstance();
        }
    };

    static {
        System.setProperty(DTM_MANAGER_NAME, DTM_MANAGER_VALUE);

        // Set up parser
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);

        try {
            parser = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            log.error("FATAL: Cannot function without XML parser", e);
            AbstractMain.shutdown(1);
        }
    }

    /**
     * Use this constructor for XML manipulation only. This Outcome cannot be validate
     * nor it can be stored in ClusterStore.
     *
     * @param xml the XML string to be manipulated
     * @throws InvalidDataException there was an error parsing the XML
     */
    public Outcome(String xml) throws InvalidDataException {
        this(NONE, xml, null);
    }

    /**
     * Use this constructor for XML manipulation and validation. This Outcome cannot be stored in ClusterStore.
     *
     * @param xml the XML string to be manipulated
     * @param schema the Schema of the XML
     * @throws InvalidDataException there was an error parsing the XML
     */
    public Outcome(String xml, Schema schema) throws InvalidDataException {
        this(NONE, xml, schema);
    }

    public Outcome(int id, String xml, String schemaName, Integer schemaVersion) throws InvalidDataException {
        this(id, (Document)null, null);
        mSchemaName = schemaName;
        mSchemaVersion = schemaVersion;

        try {
            mDOM = parse(xml);
        }
        catch (IOException | SAXException ex) {
            log.error("INVALID XML - schema:"+(null == mSchema ? null : mSchema.getName())+"\n"+xml, ex);
            throw new InvalidDataException("XML not valid for schema:"+mSchema+" error:"+ex.getMessage());
        }
    }

    /**
     * Use this constructor to manipulate, validate and store this outcome
     *
     * @param id eventID
     * @param xml the XML string to be manipulated
     * @param schema the Schema of the XML
     * @throws InvalidDataException there was an error parsing the XML
     */
    public Outcome(int id, String xml, Schema schema) throws InvalidDataException {
        this(id, (Document)null, schema);

        try {
            mDOM = parse(xml);
        }
        catch (IOException | SAXException ex) {
            log.error("INVALID XML - schema:"+(null == mSchema ? null : mSchema.getName())+"\n"+xml, ex);
            throw new InvalidDataException("XML not valid for schema:"+mSchema+" error:"+ex.getMessage());
        }
    }

    /**
     * Very basic constructor to set all members
     *
     * @param id eventID
     * @param dom parsed XML Document
     * @param schema the Schema instance
     */
    public Outcome(int id, Document dom, Schema schema) {
        mID = id;
        mDOM = dom;
        mSchema = schema;
    }

    /**
     * The constructor derives all the meta data (ID and Schema) from the path
     *
     * @param path the actuals path used by the ClusterStorage
     * @param xml the XML string to parse
     * @throws PersistencyException there was DB error
     * @throws InvalidDataException  Version or EventID was an invalid number
     */
    public Outcome(String path, String xml) throws PersistencyException, InvalidDataException {
        setMetaDataFromPath(path);

        try {
            mDOM = parse(xml);
        }
        catch (IOException | SAXException ex) {
            log.error("Invalid XML", ex);
            throw new InvalidDataException("XML not valid: "+ex.getMessage());
        }
    }

    /**
     * The constructor derives all the meta data (ID and Schema) from the path
     *
     * @param path the actuals path used by the ClusterStorage
     * @param data the parsed xml Document
     * @throws PersistencyException there was DB error
     * @throws InvalidDataException  Version or EventID was an invalid number
     */
    public Outcome(String path, Document data) throws PersistencyException, InvalidDataException {
        setMetaDataFromPath(path);
        mDOM = data;
    }

    /**
     * Gets the Schema object associated with the Outcome
     * @return the Schema object associated with the Outcome
     */
    public Schema getSchema() {
        return getSchema(null);
    }

    /**
     * Gets the Schema object associated with the Outcome
     * 
     * @param transactionKey the key of the transaction
     * @return the Schema object associated with the Outcome
     */
    public Schema getSchema(TransactionKey transactionKey) {
        if (mSchema == null) {
            try {
                mSchema = LocalObjectLoader.getSchema(mSchemaName, mSchemaVersion, transactionKey);
            }
            catch (ObjectNotFoundException | InvalidDataException e) {
                log.debug("Cannot retrieve Schema object", e);
            }
        }
        return mSchema;
    }

    /**
     * Retrieves the SchemaName, Version, EevetnId triplet from the path. Check getClusterPath() implementation
     *
     * @param path the ClusterPath to work with
     * @throws PersistencyException path was incorrect
     * @throws InvalidDataException Schema was not found or the Path has incorrect data
     */
    protected void setMetaDataFromPath(String path) throws PersistencyException, InvalidDataException {
        StringTokenizer tok = new StringTokenizer(path,"/");

        if (tok.countTokens() != 3 && !(tok.nextToken().equals(OUTCOME.getName())))
            throw new PersistencyException("Outcome path must have three components:" + path);

        String schemaName = tok.nextToken();
        String verString  = tok.nextToken();
        String objId      = tok.nextToken();

        try {
            Integer schemaVersion = Integer.valueOf(verString);
            mSchema = LocalObjectLoader.getSchema(schemaName, schemaVersion);
            mID = Integer.valueOf(objId);
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Version or EventID was an invalid number version:"+verString + " eventID:" + objId);
        }
        catch (ObjectNotFoundException e) {
            log.error("", e);
            throw new InvalidDataException("Problem loading schema:"+schemaName+" version:"+verString);
        }
    }

    /**
     * Evaluates the given XPath expression thread-safely and efficiently. It starts fromt he root Node.
     *
     * @param xpathExpr the XPath expression
     * @return the result of the evaluated expression
     * @throws XPathExpressionException  If expression cannot be compiled.
     */
    public Object evaluateXpath(String xpathExpr, QName returnType) throws XPathExpressionException {
        return evaluateXpath(mDOM, xpathExpr, returnType);
    }

    /**
     * Evaluates the given XPath expression thread-safely and efficiently
     *
     * @param startNode the starting Node
     * @param xpathExpr the XPath expression
     * @return the result of the evaluated expression
     * @throws XPathExpressionException  If expression cannot be compiled.
     */
    public Object evaluateXpath(Node startNode, String xpathExpr, QName returnType) throws XPathExpressionException {
        XPath xpath = XPATH_FACTORY.get().newXPath();
        return xpath.compile(xpathExpr).evaluate(startNode, returnType);
    }

    /**
     * Validates the actual XML Document against the provided Schema
     *
     * @return the errors found
     * @throws InvalidDataException Schema was null
     */
    public String validate() throws InvalidDataException {
        if (mSchema == null) {
            mDOM.normalize();
            throw new InvalidDataException("Schema was NOT provided");
        }

        OutcomeValidator validator = OutcomeValidator.getValidator(mSchema);

        if (Outcome_Validation_useDOM.getBoolean())
            return validator.validate(mDOM);
        else
            return validator.validate(getData());
    }

    /**
     * Validates the actual XML Document against the provided Schema
     *
     * @throws InvalidDataException XML document is not valid instance of the Schema
     */
    public void validateAndCheck() throws InvalidDataException {
        String error = validate();

        if (StringUtils.isNotBlank(error)) {
            log.error("Outcome not valid: " + error);
            log.error("XML: \n"+getData());
            log.error("XSD: \n"+getSchema().getXSD());
            throw new InvalidDataException(error);
        }
    }

    @Override
    public void setName(String name) {
        try {
            mID = Integer.valueOf(name);
        }
        catch (NumberFormatException e) {
            log.error("Invalid id set on Outcome:"+name);
        }
    }

    @Override
    public String getName() {
        return mID.toString();
    }

    public void setData(String xml) throws SAXException, IOException {
        mDOM = parse(xml);
    }

    /**
     * Gets the value of the given TEXT, CDATA, ATTRIBUTE or ELEMENT Node
     *
     * @param node the Node to work on
     * @return the value of the node
     * @throws InvalidDataException the Node is not a proper type
     */
    public String getNodeValue(Node node) throws InvalidDataException {
        int type = node.getNodeType();

        if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE || type == Node.ATTRIBUTE_NODE) {
            return node.getNodeValue();
        }
        else if (type == Node.ELEMENT_NODE) {
            NodeList nodeChildren = node.getChildNodes();

            if (nodeChildren.getLength() == 0) {
                log.trace("getNodeValue() - No child/text node for node:"+node.getNodeName()+" => returning null");
                //throw new InvalidDataException("No child/text node for element '"+node.getNodeName()+"'");
                return null;
            }
            else if (nodeChildren.getLength() == 1) {
                Node child = nodeChildren.item(0);

                if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE)
                    return child.getNodeValue();
                else
                    throw new InvalidDataException("Node '"+node.getNodeName()+"' can't get data from child node name:"+child.getNodeName()+" type:"+type);
            }
            else
                throw new InvalidDataException("Node '"+node.getNodeName()+"' shall have 0 or 1 children only #children:"+nodeChildren.getLength());
        }
        else
            throw new InvalidDataException("Cannot handle node name:"+node.getNodeName()+" type:"+type);
    }

    /**
     * Sets the value of the given TEXT, CDATA, ATTRIBUTE or ELEMENT Node
     *
     * @param node the Node to work on
     * @param value the value to set
     * @throws InvalidDataException the Node is not a proper type
     */
    public void setNodeValue(Node node, String value) throws InvalidDataException {
        setNodeValue(node, value, false);
    }

    /**
     * Sets the value of the given TEXT, CDATA, ATTRIBUTE or ELEMENT Node
     * 
     * @param node the Node to work on
     * @param value the value to set
     * @param useCdata force to use CDATA 
     * @throws InvalidDataException the Node is not a proper type
     */
    public void setNodeValue(Node node, String value, boolean useCdata) throws InvalidDataException {
        int type = node.getNodeType();

        if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE || type == Node.ATTRIBUTE_NODE) {
            if (useCdata && type != Node.CDATA_SECTION_NODE ) {
                throw new InvalidDataException("Node '"+node.getNodeName()+"' can't set CDATA of attribute or text node");
            }

            node.setNodeValue(value);
        }
        else if (type == Node.ELEMENT_NODE) {
            NodeList nodeChildren = node.getChildNodes();

            if (nodeChildren.getLength() == 0) {
                createNewTextOrCdataNode(node, value, useCdata);
            }
            else if (nodeChildren.getLength() == 1) {
                replaceTextOrCdataNode(node, value, useCdata);
            }
            else if (useCdata) {
                findAndReplaceCdataNode(node, value);
            }
            else {
                throw new InvalidDataException("Node '"+node.getNodeName()+"' shall use CDATA or have 0 or 1 children instead of "+nodeChildren.getLength());
            }
        }
        else if (type == Node.ATTRIBUTE_NODE) {
            if (useCdata) {
                throw new InvalidDataException("Node '"+node.getNodeName()+"' can't set CDATA for attribute");
            }

            node.setNodeValue(value);
        }
        else {
            throw new InvalidDataException("Cannot handle node name:"+node.getNodeName() + " typeCode:"+type);
        }
    }

    private void findAndReplaceCdataNode(Node node, String value) throws InvalidDataException {
        NodeList nodeChildren = node.getChildNodes();

        boolean cdataFound = false;
        for (int i = 0; i < nodeChildren.getLength(); i++) {
            Node child = nodeChildren.item(i);
            switch (child.getNodeType()) {
                case Node.TEXT_NODE:
                    //do nothing
                    break;
                case Node.CDATA_SECTION_NODE:
                    child.setNodeValue(value);
                    cdataFound = true;
                    break;

                default:
                    throw new InvalidDataException("Node '"+node.getNodeName()+"' can't update CDATA for nodeType:"+child.getNodeType());
            }
        }

        if (!cdataFound) {
            throw new InvalidDataException("Node '"+node.getNodeName()+"' could not find CDATA");
        }
    }

    private void replaceTextOrCdataNode(Node node, String value, boolean useCdata) throws InvalidDataException {
        Node child = node.getChildNodes().item(0);

        switch (child.getNodeType()) {
            case Node.TEXT_NODE:
                if (useCdata) {
                    node.replaceChild(mDOM.createCDATASection(value), child);
                    break;
                }
            case Node.CDATA_SECTION_NODE:
                child.setNodeValue(value);
                break;

            default:
                throw new InvalidDataException("Node '"+node.getNodeName()+"' can't set value for nodeType:"+child.getNodeType());
        }
    }

    private void createNewTextOrCdataNode(Node node, String value, boolean useCdata) {
        if (useCdata) node.appendChild(mDOM.createCDATASection(value));
        else          node.appendChild(mDOM.createTextNode(value));
    }

    /**
     * Retrieves the text, CDATA or attribute value of the Node selected by the XPath
     *
     * @param xpath The path to access the selected Node
     * @return the value of the selected Node
     * @throws XPathExpressionException xpath was not valid (e.g. there is no such node)
     * @throws InvalidDataException xpath result is not text, CDATA or attribute
     */
    public String getFieldByXPath(String xpath) throws XPathExpressionException, InvalidDataException {
        Node field = getNodeByXPath(xpath);

        if (field == null) throw new InvalidDataException("Cannot resolve xpath:"+xpath);
        else               return getNodeValue(field);
    }

    /**
     * Determines if the NodeList is actually a single field, an element with text data only
     *
     * @param elements NodeList
     * @return true if the NodeList has a single field of type ELEMENT_NODE
     */
    private boolean hasSingleField(NodeList elements) {
        return (elements != null && elements.getLength() > 0 && elements.item(0).getNodeType() == Node.ELEMENT_NODE);
    }

    /**
     * Sets an Attribute value by name of the given Element. It only updates existing Attributes.
     * If data is null, Element exists and the remove flag is true the node is removed.
     *
     * @param element the Element to search
     * @param name the name of the Attribute
     * @param data the value to set
     * @param remove flag to remove existing node when data is null
     * @throws InvalidDataException the attribute was not found
     */
    public void setAttribute(Element element, String name, String data, boolean remove) throws InvalidDataException {
        if (data == null && remove) {
            log.debug("setAttribute() - removing name:"+name);

            if (element.hasAttribute(name)) element.removeAttribute(name);
            return;
        }

        if (element.hasAttribute(name)) element.getAttributeNode(name).setValue(data);
        else                            throw new InvalidDataException("Invalid name:'"+name+"'");
    }

    /**
     * Sets an Attribute value by name of the given Element. It only updates existing Attributes.
     *
     * @param element the Element to search
     * @param name the name of the Attribute
     * @param data the value to set
     * @throws InvalidDataException the Attribute was not found
     */
    public void setAttribute(Element element, String name, String data) throws InvalidDataException {
        setAttribute(element, name, data, false);
    }

    /**
     * Sets the value of an attribute in the root Element. It can only update existing attributes.
     *
     * @param name the name of the Attribute
     * @param data the value to be set
     * @param remove flag to remove the element if the data is null
     * @throws InvalidDataException attribute was not found
     */
    public void setAttribute(String name, String data, boolean remove) throws InvalidDataException {
        setAttribute(mDOM.getDocumentElement(), name, data, remove);
    }

    /**
     * Sets an Attribute value by name of the root Element.
     *
     * @param name the name of the Attribute
     * @param data the value to set
     * @throws InvalidDataException the name was not found
     */
    public void setAttribute(String name, String data) throws InvalidDataException {
        setAttribute(name, data, false);
    }

    /**
     * Sets the value of an attribute in a given Field, i.e. named Element. It can only update existing attributes.
     *
     * @param field the named Element in the root Element
     * @param name the name of the Attribute
     * @param data the value to be set
     * @param remove flag to remove the element if the data is null
     * @throws InvalidDataException Element or attribute was not found
     */
    public void setAttributeOfField(String field, String name, String data, boolean remove) throws InvalidDataException {
        NodeList elements = mDOM.getDocumentElement().getElementsByTagName(field);

        if (hasSingleField(elements))
            setAttribute((Element)elements.item(0), name, data, remove);
        else
            throw new InvalidDataException("'"+field+"' is invalid or not a single field");
    }

    /**
     * Sets the value of an attribute in a given Field, i.e. named Element. It can only update existing attributes.
     *
     * @param field the named Element in the root Element
     * @param name the name of the Attribute
     * @param data the value to be set
     * @throws InvalidDataException Element or attribute was not found
     */
    public void setAttributeOfField(String field, String name, String data) throws InvalidDataException {
        setAttributeOfField(field, name, data, false);
    }

    /**
     * Sets the textNode value of the named Element of the given Element. It only updates existing Element.
     *
     * @param element Element to use
     * @param name the name of the Element
     * @param data the data to be set
     * @param remove flag to remove the element if the data is null
     * @throws InvalidDataException the name was not found or there were more Elements with the given name
     */
    public void setField(Element element, String name, String data, boolean remove) throws InvalidDataException {
        NodeList elements = element.getElementsByTagName(name);

        if (hasSingleField(elements)) {
            if (data == null && remove) {
                log.debug("setField() - removing name:"+name);
                element.removeChild(elements.item(0));
                return;
            }

            //Setting nodeValue to null could corrupt document
            if (data == null) data = "";

            setNodeValue(elements.item(0), data);
        }
        else {
            throw new InvalidDataException("'"+name+"' is invalid or not a single field");
        }
    }

    /**
     * Sets the textNode value of the named Element of the given Element. It only updates existing Element.
     *
     * @param element Element to use
     * @param name the name of the Element
     * @param data the data to be set
     * @throws InvalidDataException the name was not found or there were more Elements with the given name
     */
    public void setField(Element element, String name, String data) throws InvalidDataException {
        setField(element, name, data, false);
    }

    /**
     * Sets the textNode value of the named Element of the root Element. It only updates existing Element.
     *
     * @param name the name of the Element
     * @param data the data to be set
     * @param remove flag to remove the element if the data is null
     * @throws InvalidDataException the name was not found or there were more Elements with the given name
     */
    public void setField(String name, String data, boolean remove) throws InvalidDataException {
        setField(mDOM.getDocumentElement(), name, data, remove);
    }

    /**
     * Sets the textNode value of the named Element of the root Element. It only updates existing Element.
     *
     * @param name the name of the Element
     * @param data the data to be set
     * @throws InvalidDataException the name was not found or there were more Elements with the given name
     */
    public void setField(String name, String data) throws InvalidDataException {
        setField(name, data, false);
    }

    /**
     * Sets the text, CDATA or attribute value of the Node selected by the XPath. It only updates existing Nodes.
     *
     * @param xpath the selected Node to be updated
     * @param data string containing the data
     * @throws XPathExpressionException xpath is invalid
     * @throws InvalidDataException xpath result is not text, CDATA or attribute
     */
    public void setFieldByXPath(String xpath, String data) throws XPathExpressionException, InvalidDataException {
        setFieldByXPath(xpath, data, false);
    }

    /**
     * Sets the text, CDATA or attribute value of the Node selected by the XPath. It only updates existing Nodes.
     * If data is null and the node exists, the node is removed
     *
     * @param xpath the selected Node to be updated
     * @param data string containing the data, it can be null
     * @param remove flag to remove existing node when data is null
     * @throws XPathExpressionException xpath is invalid
     * @throws InvalidDataException xpath result is not text, CDATA or attribute
     */
    public void setFieldByXPath(String xpath, String data, boolean remove) throws XPathExpressionException, InvalidDataException {
        if (StringUtils.isBlank(xpath)) throw new InvalidDataException("Xpath is null or empty string");

        if (data == null && remove) {
            log.debug("setFieldByXPath() - removing field xpath");

            removeNodeByXPath(xpath);
            return;
        }

        //Setting nodeValue to null could corrupt document
        if (data == null) data = "";

        Node field = getNodeByXPath(xpath);

        if (field == null) {
            log.error("Xpath '"+xpath+"' is invalid", getData());
            throw new InvalidDataException("Xpath '"+xpath+"' is invalid");
        }
        else
            setNodeValue(field, data);
    }

    /**
     * Append the new Node created from xmlFragment as a child of the Node selected by the XPath
     *
     * @param xpath the selected parent node
     * @param xmlFragment string containing the xml fragment
     * @return the Node just added
     */
    public Node appendXmlFragment(String xpath, String xmlFragment) throws InvalidDataException {
        try {
            Node parentNode = getNodeByXPath(xpath);
            Node newNode = parse(xmlFragment).getDocumentElement();
            return parentNode.appendChild(mDOM.importNode(newNode, true));
        }
        catch (SAXException | IOException | XPathExpressionException e) {
            log.error("", e);
            throw new InvalidDataException(e.getMessage());
        }
    }

    /**
     * Returns the serialised DOM as a string without pretty printing-
     *
     * @return the xml string
     */
    public String getData() {
        return getData(false);
    }

    /**
     * Returns the serialised DOM as a string -
     *
     * @param prettyPrint is the string should be pretty printed or not
     * @return the xml string
     */
    public String getData(boolean prettyPrint) {
        try {
            return serialize(mDOM, prettyPrint);
        }
        catch (InvalidDataException e) {
           log.error("", e);
            return null;
        }
    }

    /**
     * Gets the name or UUID of the Schema item associated with the Outcome
     * @return the name or UUID of the Schema item associated with the Outcome
     */
    public String getSchemaName() {
        if (mSchema != null) {
            return mSchema.getName();
        }
        else if (StringUtils.isNoneBlank(mSchemaName)) {
            return mSchemaName;
        }

        throw new IllegalArgumentException("Outcome must have valid Schema");
    }

    /**
     * @deprecated use {@link Outcome#getSchemaName()} instead
     * @return
     */
    public String getSchemaType() {
        return getSchemaName();
    }

    /**
     * Gets the version of the Schema item associated with the Outcome
     * @return the version of the Schema item associated with the Outcome
     */
    public int getSchemaVersion() {
        if (mSchema != null) {
            return mSchema.getVersion();
        }
        else if (mSchemaVersion != null) {
            return mSchemaVersion;
        }

        throw new IllegalArgumentException("Outcome must have valid Schema");
    }

    /**
     * Returns {@link ClusterType#OUTCOME}
     */
    @Override
    public ClusterType getClusterType() {
        return OUTCOME;
    }

    @Override
    public String getClusterPath() {
        if (mID == null || mID == NONE || mSchema == null) throw new IllegalArgumentException("Outcome must have valid ID and Schema");

        return getClusterType()+"/"+mSchema.getName()+"/"+mSchema.getVersion()+"/"+mID;
    }

    public static Document newDocument() throws SAXException, IOException {
        return parse((InputSource)null);
    }

    /**
     * Parses the xml string into a DOM tree
     *
     * @param xml string to be parsed, can be null. When xml is null it creates empty Document.
     * @return the parsed Document
     *
     * @throws SAXException error parsing document
     * @throws IOException any IO errors occur
     */
    public static Document parse(String xml) throws SAXException, IOException {
        return parse(new InputSource(new StringReader(xml)));
    }

    /**
     * Parses the input source into a DOM tree. When input source is null it creates empty Document.
     *
     * @param xml string to be parsed, can be null.
     * @return the parsed Document
     *
     * @throws SAXException error parsing document
     * @throws IOException any IO errors occur
     */
    public static Document parse(InputSource xml) throws SAXException, IOException {
        synchronized (parser) {
            if (xml!=null) return parser.parse(xml);
            else           return parser.newDocument();
        }
    }

    /**
     * Retrieves an Attribute value by name of the given Element.
     *
     * @param element the Element to query
     * @param name The name of the attribute to retrieve.
     * @return The value as a string, or null if that attribute does not have a specified or default value.
     */
    public String getAttribute(Element element, String name) {
        String value = element.getAttribute(name);

        if (StringUtils.isNotBlank(value)) return value;
        else                               return null;
    }

    /**
     * Retrieves an Attribute value by name of the root Element.
     *
     * @param name The name of the attribute to retrieve.
     * @return The value as a string, or null if that attribute does not have a specified or default value.
     */
    public String getAttribute(String name) {
        return getAttribute(mDOM.getDocumentElement(), name);
    }

    /**
     * Retrieves an Attribute value by name from the named Element.
     *
     * @param field The name of the field.
     * @param attribute The name of the attribute to retrieve.
     * @return The value as a string, or null if that attribute does not have a specified or default value.
     */
    public String getAttributeOfField(String field, String attribute) {
        NodeList elements = mDOM.getDocumentElement().getElementsByTagName(field);

        if (hasSingleField(elements)) {
            String value = ((Element)elements.item(0)).getAttribute(attribute);

            if (StringUtils.isNotBlank(value)) return value;
            else                               return null;
        }
        else {
            log.debug("getAttributeOfField() - '{}' is invalid or not a single field", field);
            return null;
        }
    }

    /**
     * Retrieves the textNode value of the named Element of the given Element.
     *
     * @param element the Element to query
     * @param name The name of the Element
     * @return The value as a string, or null if that field does not exists
     */
    public String getField(Element element, String name) {
        try {
            NodeList elements = element.getElementsByTagName(name);
            if (hasSingleField(elements)) {
                if (elements.getLength() > 1) {
                    log.warn("getField() - '{}' was found multiple times, returning first occurance", name);
                }

                return getNodeValue(elements.item(0));
            }
            else{
                log.debug("getField() - '{}' is invalid or not a single field", name);
            }
        }
        catch (InvalidDataException e) {
            log.warn("getField() - exception caught, returning null", e);
        }

        return null;
    }

    /**
     * Retrieves the textNode value of the named Element of the root Element.
     *
     * @param name The name of the Element
     * @return The value as a string, or null if that field does not exists
     */
    public String getField(String name) {
        return getField( mDOM.getDocumentElement(), name);
    }

    /**
     * Gets a NodeList selected by the xpath
     *
     * @param xpathExpr the xpath to select the list of Nodes
     * @return NodeList
     * @throws XPathExpressionException invalid xpath
     */
    public NodeList getNodesByXPath(String xpathExpr) throws XPathExpressionException {
        return (NodeList) evaluateXpath(xpathExpr, XPathConstants.NODESET);
    }

    /**
     * Gets a List selected by the xpath
     *
     * @param xpathExpr the xpath to select the list of Nodes
     * @return Node
     * @throws XPathExpressionException invalid xpath
     */
    public Node getNodeByXPath(String xpathExpr) throws XPathExpressionException {
        return (Node) evaluateXpath(xpathExpr, XPathConstants.NODE);
    }

    /**
     * Removes the node selected by the xpath
     *
     * @param xpathExpr xpath to select the Node
     * @return the Node removed
     * @throws XPathExpressionException invalid xpath
     * @throws InvalidDataException invalid xpath
     */
    public Node removeNodeByXPath(String xpathExpr) throws XPathExpressionException, InvalidDataException {
        if (StringUtils.isBlank(xpathExpr)) throw new InvalidDataException("Xpath is null or empty string");

        Node nodeToTemove = getNodeByXPath(xpathExpr);

        if (nodeToTemove == null) {
            log.error("Xpath '"+xpathExpr+"' is invalid\n" + getData());
            throw new InvalidDataException("Xpath '"+xpathExpr+"' is invalid");
        }

        return nodeToTemove.getParentNode().removeChild(nodeToTemove);
    }

    private static Transformer getPrettyTransformer() throws InvalidDataException {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute("indent-number", 2);

            // add XSLT for pretty print
            InputStream is = Outcome.class.getResourceAsStream("/org/cristalise/kernel/utils/resources/textFiles/prettyPrint.xslt");
            Transformer transformer = tf.newTransformer(new StreamSource(is));
    
            // add extra standalone to break the root node to a new line
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

            return transformer;
        }
        catch (Exception ex) {
            log.error("getPrettyTransformer()", ex);
            throw new InvalidDataException(ex);
        }
    }

    private static Transformer getTransformer() throws InvalidDataException {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer             transformer = tf.newTransformer();
    
            return transformer;
        }
        catch (Exception ex) {
            log.error("getTransformer()", ex);
            throw new InvalidDataException(ex);
        }
    }

    /**
     * Serialize the given Document
     *
     * @param doc document to be serialized
     * @param prettyPrint if the xml is pretty printed or not
     * @return the xml string
     * @throws InvalidDataException Transformer Exception
     */
    public static String serialize(Node node, boolean prettyPrint) throws InvalidDataException {
        try {
            Transformer transformer = prettyPrint ? getPrettyTransformer() : getTransformer();

            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(out));
            return out.toString();
        }
        catch (Exception ex) {
            log.error("serialize()", ex);
            throw new InvalidDataException(ex);
        }
    }

    /**
     * 
     */
    public static void traverseChildElements(Node node, Consumer<Node> action) {
        NodeList childNodes = node.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                action.accept(childNodes.item(i));
            }
        }
    }

    /**
     * Reads the all Attributes and child Elements of the given Node. Use TreeMap to keep the order of Nodes.
     *
     * @param node the node to work with
     * @return a Map as a key/value pairs of Attribute/Element names with their value
     */
    public Map<String, String> getRecordOfNode(Node node) {
        final Map<String, String> record = new TreeMap<>();

        traverseChildElements(node, (element) -> {
            String name = element.getNodeName();
            String value = element.getTextContent();

            record.put(name, value);
        });

        NamedNodeMap attrs = node.getAttributes();

        for (int i = 0; i < attrs.getLength(); i++) {
            String name = attrs.item(i).getNodeName();
            String value = attrs.item(i).getTextContent();

            record.put(name, value);
        }

        return record;
    }

    /**
     * Reads the all Attributes and child Elements of the root Element
     *
     * @return a Map as a key/value pairs of Attribute/Element names with their value
     */
    public Map<String, String> getRecord() {
        return getRecordOfNode( mDOM.getDocumentElement() );
    }

    /**
     * Reads the all Attributes and child Elements of the Node selected by the xpath
     *
     * @param xpath the xpath pointing to the Node
     * @return a Map as a key/value pairs of Attribute/Element names with their value
     * @throws XPathExpressionException xpath is invalid
     */
    public Map<String, String> getRecord(String xpath) throws XPathExpressionException {
        return getRecordOfNode( getNodeByXPath(xpath) );
    }

    /**
     * Reads all Attributes and child Elements of the list of Node selected by the xpath
     *
     * @param xpath selecting the list of Nodes
     * @return List of Maps as a key/value pairs of Attribute/Element names with their value
     * @throws XPathExpressionException xpath is invalid
     */
    public List<Map<String, String>> getAllRecords(String xpath) throws XPathExpressionException {
        List< Map<String, String> > records = new ArrayList<>();

        NodeList nodes = getNodesByXPath(xpath);

        for (int i = 0; i < nodes.getLength(); i++) records.add( getRecordOfNode(nodes.item(i)) );

        return records;
    }

    /**
     * Reads list of values of the Attributes and child Elements of the given Element.
     * The values are returned in the order specified in the names parameter. It only return values for the list of names.
     * Null is added to the result if there is no value for the given name.
     *
     * @param element the Element to use
     * @param names the the Attributes and Element names to retrieve
     * @return List of values
     */
    public List<String> getRecordOfElement(Element element, List<String> names) {
        List<String> record = new ArrayList<>();

        for (String name : names) {
            String value = getField(element, name);

            if (value == null)  value = getAttribute(element, name);

            record.add(value);
        }
        return record;
    }

    /**
     * Reads list of values of the Attributes and child Elements of the root Element.
     * The values are returned in the order specified in the names parameter. It only return values for the list of names.
     * Null is added to the result if there is no value for the given name.
     *
     * @param names the the Attributes and Element names to retrieve
     * @return List of values
     */
    public List<String> getRecord(List<String> names) {
        List<String> record = new ArrayList<>();

        for (String name : names) {
            String value = getField(name);

            if (value == null) value = getAttribute(name);

            record.add(value);
        }

        return record;
    }

    /**
     * Reads list of values of the Attributes and child Elements of the Element selected by the xpath.
     * The values are returned in the order specified in the names parameter. It only return values for the list of names.
     * Null is added to the result if there is no value for the given name.
     *
     * @param xpath to select the Element
     * @param names the the Attributes and Element names to retrieve
     * @return List of values
     * @throws XPathExpressionException invalid xpath
     */
    public List<String> getRecord(String xpath, List<String> names) throws XPathExpressionException {
        return getRecordOfElement((Element)getNodeByXPath(xpath), names);
    }

    /**
     * Reads list of list of values of the Attributes and child Elements of the list of Elements selected by the xpath.
     * The values are returned in the order specified in the names parameter. It only return values for the list of names.
     * Null is added to the result if there is no value for the given name.
     *
     * @param xpath to select the list of Element
     * @param names the the Attributes and Element names to retrieve
     * @return List of list of values
     * @throws XPathExpressionException invalid xpath
     */
    public List<List<String>> getAllRecords(String xpath, List<String> names) throws XPathExpressionException {
        List< List<String> > records = new ArrayList<>();
        NodeList nodes = getNodesByXPath(xpath);

        for (int i = 0; i < nodes.getLength(); i++) records.add( getRecordOfElement((Element)nodes.item(i), names) );

        return records;
    }

    /**
     * Sets the values of Attributes and child Element of the root Element. It only updates existing elements.
     *
     * @param record Map with a key/value pairs to find the fields or attributes to update
     * @throws InvalidDataException the name in the map was invalid
     */
    public void setRecord(Map<String, String> record) throws InvalidDataException {
        setRecord(mDOM.getDocumentElement(), record);
    }

    /**
     * Sets the values of Attributes and child Element of the Element selected by xpath. It only updates existing elements.
     *
     * @param xpath apth to the Element to be updated
     * @param record Map with a key/value pairs to find the fields or attributes to update
     * @throws InvalidDataException the name in the map was invalid
     * @throws XPathExpressionException the xpath was invalid
     */
    public void setRecord(String xpath, Map<String, String> record) throws InvalidDataException, XPathExpressionException {
        setRecord((Element)getNodeByXPath(xpath), record);
    }

    /**
     * Sets the values of Attributes and child Element of given Element. It only updates existing elements.
     *
     * @param element the element to be updated
     * @param record Map with a key/value pairs to find the fields or attributes to update
     * @throws InvalidDataException the name in the map was invalid
     */
    public void setRecord(Element element, Map<String, String> record) throws InvalidDataException {
        for (Entry<String,String> entry : record.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            try {
                setField(element, name, value);
            }
            catch (InvalidDataException e) {
                setAttribute(element, name, value);
            }
        }
    }

    /**
     * Checks if the Outcome is identical with the given input
     *
     * @param other the other Outcome to be compare with
     * @return true if the two Outcomes are identical, otherwise returns false
     */
    public boolean isIdentical(Outcome other) {
        return isIdentical(getDOM(), other.getDOM());
    }

    /**
     * Utility method to comare 2 XML Documents
     *
     * @param origDocument XML document
     * @param otherDocument the other XML document
     * @return true if the two XML Documents are identical, otherwise returns false
     */
    public static boolean isIdentical(Document origDocument, Document otherDocument) {
        Diff xmlDiff = DiffBuilder.compare(origDocument).withTest(otherDocument)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreComments()
                .ignoreWhitespace()
                .checkForSimilar()
                .build();

        if (xmlDiff.hasDifferences()) {
            Iterator<Difference> allDiffs = xmlDiff.getDifferences().iterator();

            for (int i = 1; allDiffs.hasNext(); i++) log.info("Diff #{}:{}", i, allDiffs.next());

            try {
                log.debug("expected:{}", serialize(origDocument, false));
                log.debug("actual:{}", serialize(otherDocument, false));
            }
            catch (InvalidDataException e) {}

            return false;
        }
        else
            return true;
    }

    public boolean hasField(String name) {
        return  hasField(mDOM.getDocumentElement(), name);
    }

    public boolean hasField(Element element, String name) {
        return hasSingleField(element.getElementsByTagName(name));
    }

    public String getRootName() {
        return mDOM.getDocumentElement().getNodeName();
    }

    @Override
    public String toString() {
        return getData(true);
    }
}
