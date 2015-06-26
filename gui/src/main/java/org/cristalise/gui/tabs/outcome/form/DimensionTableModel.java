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
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.table.AbstractTableModel;

import org.cristalise.gui.tabs.outcome.OutcomeException;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.Annotated;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ContentModelGroup;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Group;
import org.exolab.castor.xml.schema.Order;
import org.exolab.castor.xml.schema.Particle;
import org.exolab.castor.xml.schema.SimpleType;
import org.exolab.castor.xml.schema.SimpleTypesFactory;
import org.exolab.castor.xml.schema.XMLType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


public class DimensionTableModel extends AbstractTableModel {

    ElementDecl model;
    ArrayList<String> columnHeadings = new ArrayList<String>();
    ArrayList<Class<?>> columnClasses = new ArrayList<Class<?>>();
    ArrayList<Annotated> columnDecls = new ArrayList<Annotated>();
    ArrayList<Boolean> colReadOnly = new ArrayList<Boolean>();
    ArrayList<String> colHelp = new ArrayList<String>();
    ArrayList<Object[]> rows = new ArrayList<Object[]>();
    ArrayList<Element> elements = new ArrayList<Element>();
    boolean readOnly;

    public DimensionTableModel(ElementDecl model, boolean readOnly) throws StructuralException {
        XMLType modelContent = model.getType();
        this.model = model;
        this.readOnly = readOnly;
        // use text node for simple types
        if (modelContent.isSimpleType()) {
            SimpleType elementType = (SimpleType)modelContent;
            SimpleType baseType = elementType.getBuiltInBaseType();
            addColumn(model.getName(), baseType, baseType.getTypeCode(), new Boolean(model.getFixedValue() != null));
        }
        else if (modelContent.isComplexType()) {  // if complex type, process child elements
            ComplexType elementType = (ComplexType)modelContent;

            // find out if a CDATA type is used for this complex type
            XMLType baseType = elementType.getBaseType();
            while (!(baseType instanceof SimpleType) && baseType != null) {
                baseType = baseType.getBaseType();
            }
            if (baseType != null) {
                int typeCode = ((SimpleType)baseType).getTypeCode();
                addColumn(model.getName(), baseType, typeCode, new Boolean(model.getFixedValue() != null));
            }
            // process attributes
            for (Enumeration<?> e = elementType.getAttributeDecls(); e.hasMoreElements();) {
                AttributeDecl thisAttr = (AttributeDecl)e.nextElement();
                // HACK: if we don't resolve the reference, the type will be null
                if (thisAttr.isReference()) thisAttr = thisAttr.getReference();
                if (thisAttr.getSimpleType() == null)
                	throw new StructuralException("Attribute "+thisAttr.getName()+" in "+model.getName()+" has no type");
                addColumn(thisAttr.getName(), thisAttr, thisAttr.getSimpleType().getTypeCode(), new Boolean(thisAttr.isFixed()));
            }

            // enumerate child elements
            enumerateElements(elementType);
        }
    }

    public synchronized void addColumn(String heading, Annotated decl, int typeCode, Boolean readOnly) {
        Logger.msg(8, "Column "+heading+" contains "+decl.getClass().getName()+" readOnly="+readOnly.toString());
        columnHeadings.add(heading);
        columnDecls.add(decl);
        columnClasses.add(OutcomeStructure.getJavaClass(typeCode));
        colReadOnly.add(readOnly);

        // read help
        String helpText;
        if (decl instanceof SimpleType)
            helpText = OutcomeStructure.extractHelp(model);
        else
            helpText = OutcomeStructure.extractHelp(decl);

		if (helpText.length() == 0)
            helpText = "<i>No help is available for this cell</i>";

        colHelp.add(helpText);

    }


    public void enumerateElements(ContentModelGroup group) throws StructuralException {
        for (Enumeration<?> childElements = group.enumerate(); childElements.hasMoreElements(); ) {
            Particle thisParticle = (Particle)childElements.nextElement();
            String extraHeader = "";
            if (thisParticle instanceof Group) {
                Group thisGroup = (Group)thisParticle;
                Order order = thisGroup.getOrder();
                if (order == Order.sequence || order == Order.all)
                    enumerateElements(thisGroup);
                else // we only support sequences in data structures such as these
                    throw new StructuralException("Element "+thisGroup.getName()+". Expecting sequence or all. Got "+thisGroup.getOrder());
            }
            else if (thisParticle instanceof ElementDecl) {
                ElementDecl thisElement = (ElementDecl)thisParticle;
                int typeCode = SimpleTypesFactory.INVALID_TYPE;
                //make sure not too complex
                if (thisElement.getType() != null) {
                    if (thisElement.getType().isComplexType()) {
                        ComplexType elementType = (ComplexType)thisElement.getType();
                        if (elementType.getParticleCount() > 0 ||
                            thisElement.getMaxOccurs() > 1)
                            throw new StructuralException("Too deep for a table");
                        for (Enumeration<?> attrs = elementType.getAttributeDecls(); attrs.hasMoreElements();) {
                            AttributeDecl thisAttr = (AttributeDecl)attrs.nextElement();
                            if (!thisAttr.isFixed())
                                throw new StructuralException("Non-fixed attributes of child elements not supported in tables.");
                            else
                                extraHeader=extraHeader+" ("+thisAttr.getName()+":"+(thisAttr.getFixedValue()!=null?thisAttr.getFixedValue():thisAttr.getDefaultValue())+")";
                        }
                        // find type
                        XMLType parentType = thisElement.getType();
                        while (!(parentType instanceof SimpleType) && parentType != null) {
                            parentType = parentType.getBaseType();
                        if (parentType != null) typeCode = ((SimpleType)parentType).getTypeCode();
                        }
                    }
                    else
                        typeCode = ((SimpleType)thisElement.getType()).getTypeCode();
                }

                //add to list
                addColumn(thisElement.getName()+extraHeader, thisElement, typeCode, new Boolean(thisElement.getFixedValue() != null));
            }
            else throw new StructuralException("Particle "+thisParticle.getClass()+" not implemented");
        }
    }

    public void addInstance(Element myElement, int index) throws OutcomeException {
        if (index == -1) index = elements.size();
        Object[] newRow = new Object[columnHeadings.size()];
        for (int i=0; i<columnDecls.size(); i++) {
            if (columnDecls.get(i) instanceof ElementDecl) { // sub element - get the node from it
                ElementDecl thisElementDecl = (ElementDecl)columnDecls.get(i);
                NodeList childElements = myElement.getElementsByTagName(thisElementDecl.getName());
                switch (childElements.getLength()) {
                    case 1: // element exists - read the contents
                    Element childElement = (Element)childElements.item(0);
                    if (childElement.hasChildNodes()) {
                        Node thisNode = childElement.getFirstChild();
                        if (thisNode.getNodeType() == Node.TEXT_NODE)
                            newRow[i] = OutcomeStructure.getTypedValue(((Text)thisNode).getData(), columnClasses.get(i));
                        else
                            throw new StructuralException("First child of Field " + thisElementDecl.getName() + " was not Text. (NodeType:"+thisNode.getNodeType()+")");
                    }
                    else { // create text node
                        newRow[i] = this.setupDefaultElement(thisElementDecl, childElement, columnClasses.get(i));
                    }
                    break;
                    case 0: // element is missing - create it
                    Element newElement = myElement.getOwnerDocument().createElement(thisElementDecl.getName());
                    myElement.appendChild(newElement); //TODO: not in the right place in sequence. should insert it
                    newRow[i] = setupDefaultElement(thisElementDecl, newElement, columnClasses.get(i));
                    break;
                    default:
                    throw new CardinalException("Element "+thisElementDecl.getName()+" appeared more than once.");
                }
            }
            else if (columnDecls.get(i) instanceof AttributeDecl) { //attribute
                AttributeDecl thisAttrDecl = (AttributeDecl)columnDecls.get(i);
                newRow[i] = OutcomeStructure.getTypedValue(myElement.getAttribute(thisAttrDecl.getName()), columnClasses.get(i));
            }
            else { // first child node
                Node thisNode = myElement.getFirstChild();
                if (thisNode == null) {
                    thisNode = myElement.getOwnerDocument().createTextNode("");
                    myElement.appendChild(thisNode);
                }
                if (thisNode.getNodeType() == Node.TEXT_NODE || thisNode.getNodeType() == Node.CDATA_SECTION_NODE)
                    newRow[i] = OutcomeStructure.getTypedValue(((Text)thisNode).getData(), columnClasses.get(i));
                else
                    throw new StructuralException("First child of Column " + myElement.getTagName() + " was not Text");
            }
        }
        elements.add(index, myElement);
        rows.add(index, newRow);
        fireTableRowsInserted(index, index);
    }
    @Override
	public Class<?> getColumnClass(int columnIndex) {
        return columnClasses.get(columnIndex);
    }

    @Override
	public String getColumnName(int columnIndex) {
        return columnHeadings.get(columnIndex);
    }

    @Override
	public int getRowCount() {
        return rows.size();
    }

    @Override
	public int getColumnCount() {
        return columnHeadings.size();
    }

    @Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
        boolean isReadOnly = readOnly || colReadOnly.get(columnIndex).booleanValue();
        return !isReadOnly;
    }

    @Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Object[] thisRow = rows.get(rowIndex);
        thisRow[columnIndex]=aValue;
        Element myElement = elements.get(rowIndex);
        // update node
            if (columnDecls.get(columnIndex) instanceof ElementDecl) { // sub element
                ElementDecl thisDecl = (ElementDecl)columnDecls.get(columnIndex);
                NodeList childElements = myElement.getElementsByTagName(thisDecl.getName());
                // depend on one element with a Text child - this should have been enforced on init.
                Text childNode = (Text)(childElements.item(0).getFirstChild());
                childNode.setData(aValue.toString());
            }
            else if (columnDecls.get(columnIndex) instanceof AttributeDecl) { //attribute
                AttributeDecl thisDecl = (AttributeDecl) columnDecls.get(columnIndex);
                myElement.setAttribute(thisDecl.getName(), aValue.toString());
            }
            else { // first child node
                Text textNode = (Text)myElement.getFirstChild();
                textNode.setData(aValue.toString());
            }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public Element removeRow(int rowIndex) {
        Element elementToGo = elements.get(rowIndex);
        elements.remove(rowIndex);
        rows.remove(rowIndex);
        fireTableRowsDeleted(rowIndex,rowIndex);
        return elementToGo;
    }

    public Object setupDefaultElement(ElementDecl thisDecl, Element parent, Class<?> type) {
        Object newValue;
        String defaultValue = thisDecl.getFixedValue();
        if (defaultValue == null)
            defaultValue = thisDecl.getDefaultValue();
        if (readOnly)
            newValue = "";
        else
            newValue = OutcomeStructure.getTypedValue(defaultValue, type);

        Text newNode = parent.getOwnerDocument().createTextNode(newValue.toString());
        parent.appendChild(newNode);
        // fixed attributes
        try {
            ComplexType content = (ComplexType)thisDecl.getType();
           for (Enumeration<?> attrs = content.getAttributeDecls(); attrs.hasMoreElements();) {
               AttributeDecl thisAttr = (AttributeDecl)attrs.nextElement();
               parent.setAttribute(thisAttr.getName(), thisAttr.getFixedValue()!=null?thisAttr.getFixedValue():thisAttr.getDefaultValue());
           }
        } catch (ClassCastException ex) { } // only complex types have attributes
        return newValue;
    }

    @Override
	public Object getValueAt(int rowIndex, int columnIndex) {
        Object[] thisRow = rows.get(rowIndex);
        if (!(getColumnClass(columnIndex).equals(thisRow[columnIndex].getClass())))
            Logger.warning(thisRow[columnIndex]+" should be "+getColumnClass(columnIndex)+" is a "+thisRow[columnIndex].getClass().getName());
        return thisRow[columnIndex];
    }

    public String validateStructure() { // remove empty rows
        for (int j=0; j < rows.size(); j++) {
            Object[] elems = rows.get(j);
            boolean empty = true;
            for (int i = 0; i < elems.length && empty; i++)
                empty &= OutcomeStructure.isEmpty(elems[i]);
            if (empty)
                if (model.getMinOccurs() < rows.size())
                    removeRow(j);
                else
                    return "Too many empty rows in table "+model.getName();
        }
        return null;
    }

    public Element initNew(Document parent, int index) {
        if (index == -1) index = elements.size();
        Object[] newRow = new Object[columnHeadings.size()];
        Element myElement = parent.createElement(model.getName());
        for (int i=0; i<columnDecls.size(); i++) {
            if (columnDecls.get(i) instanceof ElementDecl) { // sub element
                ElementDecl childElementDecl = (ElementDecl)columnDecls.get(i);
                Element childElement = parent.createElement(childElementDecl.getName());
                Object newValue = setupDefaultElement(childElementDecl, childElement, columnClasses.get(i));
                myElement.appendChild(childElement);
                newRow[i] = newValue;
            }
            else if (columnDecls.get(i) instanceof AttributeDecl) { //attribute
                AttributeDecl thisAttrDecl = (AttributeDecl)columnDecls.get(i);
                String newValue = thisAttrDecl.getFixedValue()!=null?thisAttrDecl.getFixedValue():thisAttrDecl.getDefaultValue();
                newRow[i] = OutcomeStructure.getTypedValue(newValue, columnClasses.get(i));
                myElement.setAttribute(thisAttrDecl.getName(), newRow[i].toString());
            }
            else { // first child node
                newRow[i] = setupDefaultElement(model, myElement, columnClasses.get(i));
                }
        }
        elements.add(index,myElement);
        rows.add(index, newRow);
        fireTableRowsInserted(index,index);
        return myElement;
    }

    public String getHelp(int i) {
    	return colHelp.get(i);
    }

}
