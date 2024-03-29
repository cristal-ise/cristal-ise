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
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotated;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.AnyType;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ContentModelGroup;
import org.exolab.castor.xml.schema.Documentation;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Group;
import org.exolab.castor.xml.schema.ModelGroup;
import org.exolab.castor.xml.schema.Order;
import org.exolab.castor.xml.schema.Particle;
import org.exolab.castor.xml.schema.SimpleType;
import org.exolab.castor.xml.schema.SimpleTypesFactory;
import org.exolab.castor.xml.schema.Wildcard;
import org.exolab.castor.xml.schema.XMLType;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.extern.slf4j.Slf4j;

/**
 * contains child outcome elements and creates new ones
 */
@Slf4j
public abstract class OutcomeStructure {

    boolean isRootElement = false;

    ElementDecl model      = null;
    Element     myElement  = null;
    String      help       = null;

    ArrayList<String>                 subStructureOrder = new ArrayList<String>();
    HashMap<String, OutcomeStructure> subStructure      = new HashMap<String, OutcomeStructure>();

    public OutcomeStructure() {}

    public OutcomeStructure(Wildcard model) {
        // Wildcard cannot be stored in this.model
        subStructure = new HashMap<String, OutcomeStructure>();
        log.debug("ctor() - Creating Wildcard structure as " + this.getClass().getSimpleName());

        help = extractHelp(model);
    }

    public OutcomeStructure(ElementDecl model) {
        this.model = model;
        subStructure = new HashMap<String, OutcomeStructure>();

        log.debug("ctor() - Creating '" + model.getName() + "' structure as " + this.getClass().getSimpleName());

        help = extractHelp(model);
    }

    /**
     * After schema processing, addInstance() propagates the XML instance document down the layout. 
     * Most OutcomeStructures will throw an exception if called more than once, except Dimension, which 
     * is the only Outcome Structure to support maxOccurs>1
     *
     * @param myElement
     * @param parentDoc
     * @throws OutcomeBuilderException
     */
    public abstract void addInstance(Element myElement, Document parentDoc) throws OutcomeBuilderException;

    /**
     *
     * @param parent
     * @return
     */
    public abstract Element initNew(Document rootDocument);

    public abstract void exportViewTemplate(Writer template) throws IOException;
    public abstract Object generateNgDynamicForms(Map<String, Object> inputs, boolean withModel, boolean withLayout);
    public abstract JSONObject generateNgDynamicFormsCls();
    public abstract void addJsonInstance(OutcomeStructure parentStruct, Element parentElement, String name, Object json) 
            throws OutcomeBuilderException;

    /**
     * Finds the Field in the model of the actual element and returns it if it was an anyField.
     * There should be only one AnyField.
     * 
     * @return the AnyField of the actual element, or null
     */
    private OutcomeStructure getAnyField() {
        if (subStructure != null) {
            for (OutcomeStructure childModel: subStructure.values()) {
                if (childModel instanceof Field && childModel.isAnyField()) {
                    return childModel;
                }
            }
        }

        return null;
    }
    /**
     * Creates the named child element from the xsd model and adds it to the document. 
     * If the name was not found in the model of the actual element, checks 
     * if the actual element is an xs:anyType or contains an xs:any (anyField). 
     * If one of the above condition is true, it creates the requested element as a Field.
     * 
     * @param rootDocument the root of the dom
     * @param name the name of the elements to be added
     * @return the newly create element
     * @throws OutcomeBuilderException no element exists in the xsd model
     */
    public Element createChildElement(Document rootDocument, String name) throws OutcomeBuilderException {
        OutcomeStructure childModel = getChildModelElement(name);
        Element newElement = null;

        if (childModel == null) {
            if (isAnyType()) {
                newElement = rootDocument.createElement(name);
            }
            else {
                childModel = getAnyField();

                if (childModel == null) {
                    throw new StructuralException("'"+model.getName()+"' does not have child '"+name+"' declaration");
                }

                newElement = ((Field)childModel).initNewAny(rootDocument, name);
            }
        }
        else {
            newElement = childModel.initNew(rootDocument);
        }

        addChildElement(name, newElement);
        return newElement;
    }

    /**
     * Adds the child element az the correct position using the expected sequence of elements (subStructureOrder)
     * 
     * @param name the name of the elements to be added
     * @param newElement the new xml element to be added
     */
    public void addChildElement(String name, Element newElement) {
        Element refElement = null;
        boolean cont = true;

        // lets find out where to insert this new element
        for (int i = 0; i < subStructureOrder.size()-1 && cont; i++) {
            if (name.equals(subStructureOrder.get(i))) {
                cont = false;

                for (int k = i+1; k < subStructureOrder.size() && refElement == null; k++) {

                    String refElementName = subStructureOrder.get(k);
                    NodeList children = myElement.getChildNodes();

                    for (int j = 0; j < children.getLength() && refElement == null; j++) {
                        Node child = children.item(j);
                        // ignore any Node (e.g. Text) which are not Element type
                        if (child instanceof Element && child.getNodeName().equals(refElementName)) {
                            refElement = (Element) child;
                        }
                    }
                }
            }
        }

        if (refElement == null) myElement.appendChild(newElement);
        else                    myElement.insertBefore(newElement, refElement);
    }

    /**
     * Creates OutcomeStructure (element) to represent the chosen Element declaration:
     * <ol>
     * <li>if maxOccurs > 1 then Dimension
     * <li>SimpleTypes are Fields
     * <li>AnyTypes are Fields
     * <li>No element children is a Field
     * <li>Everything else is a DataRecord
     * </ol>
     */
    public OutcomeStructure createStructure(ElementDecl model) throws OutcomeBuilderException {
        XMLType elementType = model.getType();

        if (model.getMaxOccurs() == 0) return null;

        // if more than one can occur - dimension
        if (model.getMaxOccurs() > 1 || model.getMaxOccurs() == Particle.UNBOUNDED) // || model.getMinOccurs() == 0
            return new Dimension(model);

        // must have a type from now on
        if (elementType == null)
            throw new StructuralException("Element " + model.getName() + " is elementary yet has no type.");

        // simple types will be fields
        if (elementType instanceof SimpleType) {
            return new Field(model);
        }
        else if (elementType instanceof AnyType) {
            return new Field(model);
        }
        else {
            // otherwise is a complex type
            try {
                ComplexType elementComplexType = (ComplexType) elementType;

                // when no element children - field
                if (elementComplexType.getParticleCount() == 0) return new Field(model);

                // everything else is a data record
                return new DataRecord(model);
            }
            catch (ClassCastException e) {
                throw new StructuralException("Unknown XMLType for element " + model.getName(), e);
            }
        }
    }

    public OutcomeStructure createStructure(Wildcard model) throws OutcomeBuilderException {
        if (model.getMaxOccurs() == 0) return null;
        return new Field(model);
    }

    /**
     * Extracts child Element declarations from a content group and recursively from any group (not Element) of that group. calls
     * createStructure() to find the corresponding OutcomeStructure then adds it to this structure.
     */
    public void enumerateElements(ContentModelGroup group) throws OutcomeBuilderException {
        // process base types first if complex type
        // HACK: castor does not include elements from basetype, so we do it manually. if they fix it, this will duplicate child elements.
        if (group instanceof ComplexType) {
            XMLType base = ((ComplexType) group).getBaseType();

            if (base instanceof ComplexType) enumerateElements((ComplexType) base);
        }

        for (Enumeration<?> elements = group.enumerate(); elements.hasMoreElements();) {
            Particle thisParticle = (Particle) elements.nextElement();
            if (thisParticle instanceof Group) {
                Group thisGroup = (Group) thisParticle;
                if (thisGroup instanceof ModelGroup) {
                    // HACK: Castor strangeness - model groups don't seem to resolve their own references. If fixed, this will still work
                    ModelGroup thisModel = (ModelGroup) thisGroup;
                    if (thisModel.hasReference()) thisGroup = thisModel.getReference();
                }

                // xs:sequences and xs:all is supported in data structures such as these
                Order thisOrder = thisGroup.getOrder();
                if (thisOrder == Order.sequence || thisOrder == Order.all) {
                    enumerateElements(thisGroup);
                }
                else {
                    throw new StructuralException("The '" + thisGroup.getOrder() + "' group is not supported");
                }
            }
            else if (thisParticle instanceof ElementDecl) {
                ElementDecl thisElement = (ElementDecl) thisParticle;
                addStructure(createStructure(thisElement));
            }
            else if (thisParticle instanceof Wildcard) {
                log.debug("enumerateElements() - name:{} has group with Wildcard representing xs:any", getName());
                addStructure(createStructure((Wildcard)thisParticle));
            }
            else {
                throw new StructuralException("Cannot process Particle '" + thisParticle.getClass() + "' : Not implemented");
            }
        }
    }

    /**
     * Adds a generated OutcomeStructure as a child of this one. A separate structure as is often overridden.
     */
    public void addStructure(OutcomeStructure newElement) throws OutcomeBuilderException {
        if (newElement == null) return;

        String elementName = newElement.getName();

        subStructure.put(elementName, newElement);
        subStructureOrder.add(elementName);
    }

    public Element getElement() {
        return myElement;
    }

    public String getName() {
        if (model == null) return null;
        return model.getName();
    }

    public ElementDecl getModel() {
        return model;
    }

    public String getHelp() {
        return help;
    }

    public String validateStructure() {
        StringBuffer errors = new StringBuffer();

        for (Entry<String, OutcomeStructure> element : subStructure.entrySet()) {
            log.debug("validateStructure() - validating : " + element.getKey());
            errors.append(element.getValue().validateStructure());
        }

        return errors.toString();
    }

    public OutcomeStructure getChildModelElement(String name) {
        return subStructure.get(name);
    }

    public static String extractHelp(Annotated model) {
        Enumeration<?> e = model.getAnnotations();
        StringBuffer doc = new StringBuffer();

        if (e.hasMoreElements()) { // look for HTML
            Annotation note = (Annotation) e.nextElement();

            for (Enumeration<?> g = note.getDocumentation(); g.hasMoreElements();) {
                Documentation thisDoc = (Documentation) g.nextElement();

                for (Enumeration<?> h = thisDoc.getObjects(); h.hasMoreElements();) {
                    AnyNode node = (AnyNode) h.nextElement();
                    String line = node.toString();

                    if (line.length() == 0) line = node.getStringValue();
                    if (line.length() > 0)  doc.append(line).append("\n");
                }
            }
        }
        return doc.toString();
    }

    public static Class<?> getJavaClass(int typeCode) {
        switch (typeCode) {
            // boolean
            case SimpleTypesFactory.BOOLEAN_TYPE:
                return Boolean.class;

                // all types of integers
            case SimpleTypesFactory.INTEGER_TYPE:
            case SimpleTypesFactory.NON_POSITIVE_INTEGER_TYPE:
            case SimpleTypesFactory.NEGATIVE_INTEGER_TYPE:
            case SimpleTypesFactory.NON_NEGATIVE_INTEGER_TYPE:
            case SimpleTypesFactory.POSITIVE_INTEGER_TYPE:
            case SimpleTypesFactory.INT_TYPE:
            case SimpleTypesFactory.UNSIGNED_INT_TYPE:
            case SimpleTypesFactory.SHORT_TYPE:
            case SimpleTypesFactory.UNSIGNED_SHORT_TYPE:
            case SimpleTypesFactory.LONG_TYPE:
            case SimpleTypesFactory.UNSIGNED_LONG_TYPE:
            case SimpleTypesFactory.BYTE_TYPE:
            case SimpleTypesFactory.UNSIGNED_BYTE_TYPE:
                return BigInteger.class;

                // all types of floats
            case SimpleTypesFactory.FLOAT_TYPE:
            case SimpleTypesFactory.DOUBLE_TYPE:
            case SimpleTypesFactory.DECIMAL_TYPE:
                return BigDecimal.class;

                // images
            case SimpleTypesFactory.BASE64BINARY_TYPE:
            case SimpleTypesFactory.HEXBINARY_TYPE:
                return null;
                //return ImageIcon.class;

            case SimpleTypesFactory.DATE_TYPE:
                return LocalDate.class;

            case SimpleTypesFactory.TIME_TYPE:
                return OffsetTime.class;

            case SimpleTypesFactory.DATETIME_TYPE:
                return OffsetDateTime.class;

                // everything else is a string for now
            default:
                return String.class;
        }
    }

    public static Object getTypedValue(String value, Class<?> type) {
        try {
            if (type.equals(Boolean.class)) {
                if (StringUtils.isBlank(value)) return Boolean.FALSE;
                else                            return Boolean.valueOf(value);
            }
            else if (type.equals(BigInteger.class)) {
                if (StringUtils.isBlank(value)) return new BigInteger("0");
                else                            return new BigInteger(value);
            }
            else if (type.equals(BigDecimal.class)) {
                if (StringUtils.isBlank(value)) return new BigDecimal(0);
                else                            return new BigDecimal(value);
            }
        }
        catch (Exception ex) {
            log.warn("Cannot convert value '" + value + "' to a " + type.getName());
        }

        return value == null ? "" : value;
    }

    public static boolean isEmpty(Object value) {
        if (value == null) return true;

        if (value instanceof String) {
            if (((String) value).length() == 0) return true;
        }
        else if (value instanceof Boolean) {
            if (((Boolean) value).booleanValue() == false) return true;
        }
        else if (value instanceof BigInteger) {
            if (((BigInteger) value).intValue() == 0) return true;
        }
        else if (value instanceof BigDecimal) {
            if (((BigDecimal) value).floatValue() == 0.0) return true;
        }
        return false;
    }

    public boolean isOptional() {
        return model.getMinOccurs() == 0;
    }

    /**
     * this,model == null indicates that the structure is AnyField (i.e. used Wildcard representing xs:any)
     * @return true if field represents xs:any
     */
    public boolean isAnyField() {
        return this.model == null;
    }

    /**
     * @return true if field has xs:anyType
     */
    public boolean isAnyType() {
        if (model == null) return false;
        return             model.getType() instanceof AnyType;
    }

    public OutcomeStructure find(String[] names) {
        OutcomeStructure child = getChildModelElement(names[0]);

        if (names.length == 1) return child;
        else                   return child.find(Arrays.copyOfRange(names, 1, names.length-1));
    }

    public boolean isRootElement() {
        return isRootElement;
    }

    public void setRootElementFlag(boolean isRootElement) {
        this.isRootElement = isRootElement;
    }
}
