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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cristalise.gui.ImageLoader;
import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotated;
import org.exolab.castor.xml.schema.Annotation;
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
import org.exolab.castor.xml.schema.XMLType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


// contains child outcome elements - creates new ones
public abstract class OutcomeStructure extends JPanel {

    ElementDecl model;
    Element myElement = null;
    HashMap<String, Class<?>> specialEditFields;
    boolean readOnly;
    HashMap<String, OutcomeStructure> subStructure = new HashMap<String, OutcomeStructure>();
    ArrayList<String> order = new ArrayList<String>();
    static public ImageIcon helpIconSmall = ImageLoader.findImage("query_16.png");
    String help = null;
    boolean deferChild = false;
    
    public OutcomeStructure(ElementDecl model, boolean readOnly , HashMap<String, Class<?>> specialControls) {
        this.model = model;
        this.readOnly = readOnly;
        this.specialEditFields = specialControls;
        subStructure = new HashMap<String, OutcomeStructure>();
        Logger.msg(8, "Creating " + model.getName() + " structure as " +
            this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1));

        String doc = extractHelp(model);
		if (doc.length() > 0) help = doc;
    }

    public boolean getReadOnly() {
        return readOnly;
    }
    /** Contains the rules for deciding which OutcomeStructure will represent a chosen Element Declaration.
     * In this order
     * <ol>
     * <li>if maxOccurs>1 then Dimension
     * <li> SimpleTypes are Fields
     * <li> No element children is a Field
     * <li> Everything else is a DataRecord
     * </ol>
     */
    public OutcomeStructure createStructure(ElementDecl model, boolean readOnly) throws OutcomeException {
        XMLType elementType = model.getType();
        ComplexType elementComplexType;

        if (model.getMaxOccurs() == 0) return null;

        // if more than one can occur - dimension
        if (model.getMaxOccurs() > 1
                || model.getMaxOccurs() == Particle.UNBOUNDED
                || model.getMinOccurs() == 0)
            return new Dimension(model, readOnly, specialEditFields);

        // must have a type from now on
        if (elementType == null)
            throw new StructuralException("Element "+model.getName()+" is elementary yet has no type.");
        // simple types will be fields
        if (elementType instanceof SimpleType) return new Field(model, readOnly, specialEditFields);

        // otherwise is a complex type
        try {
            elementComplexType = (ComplexType)elementType;
        }
        catch (ClassCastException e) {
            throw new StructuralException("Unknown XMLType for element " + model.getName());
        }

        //when no element children -  field
        if (elementComplexType.getParticleCount() == 0) return new Field(model, readOnly, specialEditFields);

        //everything else is a data record
        return new DataRecord(model, readOnly, deferChild, specialEditFields);
    }

    /** Extracts child Element declarations from a content group and recursively from any group
     * (not Element) of that group. calls createStructure() to find the corresponding OutcomeStructure
     * then adds it to this structure.
     */
    public void enumerateElements(ContentModelGroup group) throws OutcomeException {

        // process base types first if complex type
        //HACK: castor does not include elements from basetype, so we do it manually. if they fix it, this will duplicate child elements.
        if (group instanceof ComplexType) {
            XMLType base = ((ComplexType)group).getBaseType();
            if (base instanceof ComplexType)
                enumerateElements((ComplexType)base);
            }

        for (Enumeration<?> elements = group.enumerate(); elements.hasMoreElements(); ) {
            Particle thisParticle = (Particle)elements.nextElement();
            if (thisParticle instanceof Group) {
                Group thisGroup = (Group)thisParticle;
                if (thisGroup instanceof ModelGroup) {
                	// HACK: Castor strangeness - model groups don't seem to resolve their own references. If fixed, this will still work
                    ModelGroup thisModel = (ModelGroup)thisGroup;
                    if (thisModel.hasReference()) thisGroup = thisModel.getReference();
                }
                Order thisOrder = thisGroup.getOrder();
                if (thisOrder == Order.sequence || thisOrder == Order.all) enumerateElements(thisGroup);
                else // we only support sequences in data structures such as these
                    throw new StructuralException("The '"+thisGroup.getOrder()+"' group is not supported");
            }
            else if (thisParticle instanceof ElementDecl) {
                ElementDecl thisElement = (ElementDecl)thisParticle;
                addStructure(createStructure(thisElement, readOnly));
            }
            else throw new StructuralException("Particle " + thisParticle.getClass() + " not implemented");
        }
    }

    /** Adds a generated OutcomeStructure as a child of this one. A separate structure as is often overridden.
     */
    public void addStructure(OutcomeStructure newElement) throws OutcomeException {
        if (newElement == null) return;
        subStructure.put(newElement.getName(), newElement);
        order.add(newElement.getName());
    }

    /** After schema processing, addInstance() propogates the XML instance document down the layout.
     * Most OutcomeStructures will throw an exception if called more than once, except Dimension, which is the only
     * Outcome Structure to support maxOccurs>1
     */
    public abstract void addInstance(Element myElement, Document parentDoc) throws OutcomeException;

    public Element getElement() {
        return myElement;
    }

    @Override
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
    
    public static JLabel makeLabel(String name, String helpText) {
    	JLabel label;
        if (helpText == null) 
        	label = new JLabel(name);
        else {
        	label = new JLabel(name, helpIconSmall, JLabel.LEFT);
        	label.setHorizontalTextPosition(JLabel.LEADING);
        	label.setToolTipText("<html>"+helpText+"</html>");
        }
        return label;
    	
    }

    public String validateStructure() {
        StringBuffer errors = new StringBuffer();
        for (OutcomeStructure element : subStructure.values()) {
            errors.append(element.validateStructure());
        }
        return errors.toString();
    }

    public abstract Element initNew(Document parent);

    public static String extractHelp(Annotated model) {
        Enumeration<?> e = model.getAnnotations();
        StringBuffer doc = new StringBuffer();
        if (e.hasMoreElements()) { // look for HTML
            Annotation note = (Annotation)e.nextElement();
            for (Enumeration<?> g = note.getDocumentation(); g.hasMoreElements();) {
                Documentation thisDoc = (Documentation)g.nextElement();
                for (Enumeration<?> h = thisDoc.getObjects(); h.hasMoreElements();) {
                    AnyNode node = (AnyNode)h.nextElement();
                    String line = node.toString();
                    if (line.length() == 0)
                        line = node.getStringValue();
                    if (line.length() > 0) {
                        doc.append(line).append("\n");
                    }
                }
            }
        }

        return doc.toString();
    }

    @Override
	public abstract void grabFocus();

    public static Class<?> getJavaClass(int typeCode) {
        switch (typeCode) {

            // boolean
            case SimpleTypesFactory.BOOLEAN_TYPE:
                return Boolean.class;

            // integers
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
            // floats
            case SimpleTypesFactory.FLOAT_TYPE:
            case SimpleTypesFactory.DOUBLE_TYPE:
            case SimpleTypesFactory.DECIMAL_TYPE:
                return BigDecimal.class;

            // images
            case SimpleTypesFactory.BASE64BINARY_TYPE:
            case SimpleTypesFactory.HEXBINARY_TYPE:
             return ImageIcon.class;

            // everything else is a string for now
            default:
                return String.class;
        }
    }

    public static Object getTypedValue(String value, Class<?> type) {
        try {
            if (type.equals(Boolean.class))
                if (value == null || value.equals(""))
                    return Boolean.FALSE;
                else
                    return Boolean.valueOf(value);
            else if (type.equals(BigInteger.class))
                if (value == null || value.equals(""))
                    return new BigInteger("0");
                else
                    return new BigInteger(value);
            else if (type.equals(BigDecimal.class))
                if (value == null || value.equals(""))
                    return new BigDecimal(0);
                else
                    return new BigDecimal(value);
        } catch (Exception ex) {
            Logger.error("Cannot convert value '"+value+"' to a "+type.getName());
        }
        return value==null?"":value;
    }

    public static boolean isEmpty(Object value) {
        if (value == null) return true;

        if (value instanceof String) {
            if (((String)value).length() == 0) return true;
        }
        else if (value instanceof Boolean) {
            if (((Boolean)value).booleanValue() == false) return true;
        }
        else if (value instanceof BigInteger) {
            if (((BigInteger)value).intValue() == 0) return true;
        }
        else if (value instanceof BigDecimal) {
            if (((BigDecimal)value).floatValue() == 0.0) return true;
        }
        return false;
    }
}
