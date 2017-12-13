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

import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;

import org.cristalise.kernel.persistency.outcomebuilder.field.StringField;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.ComplexType;
import org.exolab.castor.xml.schema.ElementDecl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AttributeList extends OutcomeStructure {

//    private ArrayList<StringField> attrSet = new ArrayList<StringField>();
    private HashMap<String, StringField> attrMap = new HashMap<>();

    public AttributeList(ElementDecl model) {
        AttributeDecl thisDecl;
        this.model = model;

        // simple types have no attributes
        if (model.getType().isSimpleType()) return;

        ComplexType content = (ComplexType)model.getType();

        for (Enumeration<AttributeDecl> fields = content.getAttributeDecls(); fields.hasMoreElements();) {
            thisDecl = (AttributeDecl)fields.nextElement();

            Logger.msg(8, "AttributeList() - attribute:"+thisDecl.getName()+" optional:"+thisDecl.isOptional());

            // FIXME: this will be overwritten by the help of next attributes
            help = OutcomeStructure.extractHelp(thisDecl);

            //Skipping optional attributes
            //if (thisDecl.isOptional()) continue;

            // Add entry
            try {
                //attrSet.add( StringField.getField(thisDecl) );
                attrMap.put(thisDecl.getName(), StringField.getField(thisDecl) );
            }
            catch (StructuralException e) {
                Logger.error(e);
            }
        }
    }

    @Override
    public void addInstance(Element newElement, Document parentDoc) throws OutcomeBuilderException {
        this.myElement = newElement;

        for (StringField thisField : attrMap.values()) {
            Logger.msg(8, "AttributeList.setInstance() - Populating Attribute "+thisField.getName());
            Attr thisAttr = myElement.getAttributeNode(thisField.getName());

            if (thisAttr == null) thisAttr = newAttribute((AttributeDecl)thisField.getModel());

            thisField.setData(thisAttr);
        }
    }

    public void validateAttributes() {
        if (model.getType().isComplexType()) {
            ComplexType content = (ComplexType)model.getType();

            for (Enumeration<?> fields = content.getAttributeDecls(); fields.hasMoreElements();) {
                AttributeDecl thisDecl = (AttributeDecl)fields.nextElement();
                String attrVal = myElement.getAttribute(thisDecl.getName());

                if (attrVal.length() == 0 && thisDecl.isOptional()) {
                    myElement.removeAttribute(thisDecl.getName());
                }
            }
        }
    }

    private Attr newAttribute(AttributeDecl attr) {
        myElement.setAttribute(attr.getName(), attr.getFixedValue() != null?attr.getFixedValue() : attr.getDefaultValue());
        return myElement.getAttributeNode(attr.getName());
    }

    private Attr initNewAttribute(AttributeDecl attrDecl, boolean skipOptional) throws OutcomeBuilderException {
        // Skip optional attributes
        if (attrDecl.isOptional() && skipOptional) return null;

        // HACK: if we don't resolve the reference, the type will be null
        if (attrDecl.isReference()) attrDecl = attrDecl.getReference();

        return newAttribute(attrDecl);
    }

    private Attr initNewAttribute(String attrName, boolean skipOptional) throws OutcomeBuilderException {
        AttributeDecl attrDecl = ((ComplexType)model.getType()).getAttributeDecl(attrName);

        if (attrDecl == null) throw new InvalidOutcomeException("Unknown attributeDecl:" + attrName);

        return initNewAttribute( attrDecl, skipOptional);
    }

    /**
     * Initialise a new set of attributes from the list of StringFields created during constructor. 
     * Optional attributes are skipped.
     * 
     * @param parent
     */
    public void initNew(Element parent) {
        Logger.msg(5, "AttributeList.initNew() - Creating attributes for " + model.getName());

        this.myElement = parent;

        if (model.getType().isSimpleType()) return; // no attributes in simple types

        for (StringField thisField: attrMap.values()) {
            try {
                //create new, add into parent and fill in field
                Attr attr = initNewAttribute(thisField.getName(), true);

                //optional attribute is only created if skipOptional = false
                if (attr != null) thisField.setData(attr);
            }
            catch (Exception ex) {} // impossible name mismatch
        }
    }

    @Override
    public JSONArray generateNgDynamicForms() {
        JSONArray attrs = new JSONArray();

        for (StringField attr: attrMap.values()) attrs.put(attr.generateNgDynamicForms());

        return attrs;
    }
    
    public boolean hasAttributeDecl(String name) {
        return ((ComplexType)model.getType()).getAttributeDecl(name) != null;
    }

    @Override
    public void addJsonInstance(Element parent, String attrName, Object json) throws OutcomeBuilderException {
        myElement = parent;

        Logger.msg(5, "AttributeList.addJsonInstance() - name:'" + attrName + "'");

        AttributeDecl attrDecl = ((ComplexType)model.getType()).getAttributeDecl(attrName);

        if (attrDecl == null) throw new InvalidOutcomeException("Unknown attributeDecl:" + attrName);

        Attr newAttr = initNewAttribute(attrDecl, false);
        newAttr.setValue(json.toString());

        StringField field = attrMap.get(attrName);

        if (field == null) {
            field = StringField.getField(attrDecl);
            attrMap.put(attrName,  field);
        }

        field.setData(newAttr);
    }

    @Override
    public Element initNew(Document parent) { return null; }

    @Override
    public OutcomeStructure getChildModelElement(String name) { return null; }

    @Override
    public void exportViewTemplate(Writer template) {}

    public JSONObject generateNgDynamicFormsCls() { return null; }

}
