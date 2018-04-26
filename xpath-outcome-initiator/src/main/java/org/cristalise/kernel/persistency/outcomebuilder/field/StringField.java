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
package org.cristalise.kernel.persistency.outcomebuilder.field;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Enumeration;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.persistency.outcomebuilder.InvalidOutcomeException;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeStructure;
import org.cristalise.kernel.persistency.outcomebuilder.StructuralException;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotated;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.AppInfo;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.SimpleType;
import org.exolab.castor.xml.schema.Structure;
import org.exolab.castor.xml.schema.XMLType;
import org.exolab.castor.xml.schema.simpletypes.ListType;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Superclass for the entry field for Field and AttributeList.
 */
public class StringField {

    Node       data;
    Annotated  model;
    boolean    isValid  = true;
    boolean    isAttribute = false;
    String     name;
    SimpleType contentType;
    String     text;
    String     defaultValue;

    public StringField() {}

    private static StringField getFieldForType(SimpleType type) {
        // handle lists special
        if (type instanceof ListType) return new ArrayField(type.getBuiltInBaseType());

        // is a combobox
        if (type.hasFacet(Facet.ENUMERATION)) return new ComboField(type, null);

        //Find script to populate list of values (LOVscript)
        Enumeration<Annotation> e = type.getAnnotations();
        while (e.hasMoreElements()) {
            Annotation note = e.nextElement();

            for (Enumeration<AppInfo> f = note.getAppInfo(); f.hasMoreElements();) {
                AppInfo thisAppInfo = f.nextElement();

                for (Enumeration<?> g = thisAppInfo.getObjects(); g.hasMoreElements();) {
                    AnyNode appInfoNode = (AnyNode) g.nextElement();

                    if (appInfoNode.getLocalName().equals("ScriptList") || appInfoNode.getLocalName().equals("LDAPList")) {
                        return new ComboField(type, appInfoNode);
                    }
                }
            }
        }

        // find info on length before we go to the base type
        long length = -1;
        if      (type.getLength()    != null) length = type.getLength().longValue();
        else if (type.getMaxLength() != null) length = type.getMaxLength().longValue();
        else if (type.getMinLength() != null) length = type.getMinLength().longValue();

        // find base type if derived
        if (!type.isBuiltInType()) type = type.getBuiltInBaseType();

        // derive the class from type
        Class<?> contentClass = OutcomeStructure.getJavaClass(type.getTypeCode());

        if      (contentClass.equals(Boolean.class))        return new BooleanField();
        else if (contentClass.equals(BigInteger.class))     return new IntegerField();
        else if (contentClass.equals(BigDecimal.class))     return new DecimalField();
        else if (contentClass.equals(LocalDate.class))      return new DateField();
        else if (contentClass.equals(OffsetTime.class))     return new TimeField();
        else if (contentClass.equals(OffsetDateTime.class)) return new DateTimeField();
        else if (length > 60)                               return new LongStringField();
        else                                                return new StringField();
    }

    public static StringField getField(AttributeDecl model) throws StructuralException {
        if (model.isReference()) model = model.getReference();

        StringField newField = getFieldForType(model.getSimpleType());
        newField.setDecl(model);

        return newField;
    }

    public static StringField getField(ElementDecl model) throws StructuralException {
        try {
            XMLType baseType = model.getType();

            while (!(baseType instanceof SimpleType)) baseType = baseType.getBaseType();

            StringField newField = getFieldForType((SimpleType) baseType);

            newField.setDecl(model);
            return newField;
        }
        catch (Exception ex) {
            throw new StructuralException("No type defined in model");
        }
    }

    public void setDecl(AttributeDecl attrModel) throws StructuralException {
        this.model = attrModel;
        this.name = attrModel.getName();
        this.defaultValue = attrModel.getDefaultValue();
        this.contentType = attrModel.getSimpleType();
        this.isAttribute = true;
    }

    public void setDecl(ElementDecl elementModel) throws StructuralException {
        this.model = elementModel;
        this.name = elementModel.getName();
        this.defaultValue = elementModel.getDefaultValue();
        this.isAttribute = false;

        XMLType type = elementModel.getType();

        // derive base type
        if (type.isSimpleType()) this.contentType = (SimpleType) type;
        else                     this.contentType = (SimpleType) (type.getBaseType());

        if (this.contentType == null) throw new StructuralException("No declared base type of element");
    }

    public void setData(Attr newData) throws StructuralException {
        if (!(newData.getName().equals(name)))
            throw new StructuralException("Tried to add a " + newData.getName() + " into a " + name + " attribute.");

        this.data = newData;
        setText(newData.getValue());
    }

    public void setData(Text newData) {
        String contents = newData.getData();
        this.data = newData;
        setText(contents);
    }

    public void setData(String newData) throws InvalidOutcomeException {
        if (data == null) throw new InvalidOutcomeException("No node exists");
        setText(newData);
        updateNode();
    }

    public boolean isOptional() {
        if (isAttribute) return ((AttributeDecl)model).isOptional();
        else             return ((ElementDecl)model).getMinOccurs() == 0;
    }

    public Structure getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public Node getData() {
        return data;
    }

    /**
     * Returns the default value that is specific to the type, and required to create a valid xml.
     * 
     * @return zero length String
     */
    public String getDefaultValue() {
        return "";
    }

    public void updateNode() {
        if (data == null) return;

        if (data instanceof Text) ((Text) data).setData(getText());
        else                      ((Attr) data).setValue(getText());
    }

    public String getText() {
        return text;
    }

    public void setText(String value) {
        text = value;
    }

    public String getNgDynamicFormsControlType() {
        return "INPUT";
    }

    public JSONObject generateNgDynamicFormsCls() {
        JSONObject fieldCls = new JSONObject();

        JSONObject fieldElement = new JSONObject();
        fieldElement.put("label", "ui-widget");

        JSONObject fieldGrid = new JSONObject();
        fieldGrid.put("container", "ui-g");
        fieldGrid.put("label",     "ui-g-4");
        fieldGrid.put("control",   "ui-g-8");

        fieldCls.put("element", fieldElement);
        fieldCls.put("grid", fieldGrid);
        return fieldCls;
    }

    private void setAppInfoDynamicFormsJsonValue(AnyNode node, JSONObject json) {
        String value = node.getStringValue().trim();

        // handle Boolean only for the time being
        json.put(node.getLocalName(), Boolean.parseBoolean(value));
    }

    private void readAppInfoDynamicForms(JSONObject json) {
        Enumeration<Annotation> e = model.getAnnotations();
        while (e.hasMoreElements()) {
            Annotation note = e.nextElement();

            for (Enumeration<AppInfo> f = note.getAppInfo(); f.hasMoreElements();) {
                AppInfo thisAppInfo = f.nextElement();

                for (Enumeration<?> g = thisAppInfo.getObjects(); g.hasMoreElements();) {
                    AnyNode appInfoNode = (AnyNode) g.nextElement();

                    // add all Elements of 'dynamicForms' to json
                    if (appInfoNode.getNodeType() == AnyNode.ELEMENT && appInfoNode.getLocalName().equals("dynamicForms")) {
                        AnyNode child = appInfoNode.getFirstChild(); //stupid API, there is no getChildren

                        if (child != null) {
                            if (child.getNodeType() == AnyNode.ELEMENT) setAppInfoDynamicFormsJsonValue(child, json);

                            for (child = child.getNextSibling(); child != null; child = child.getNextSibling()) {
                                if (child.getNodeType() == AnyNode.ELEMENT) setAppInfoDynamicFormsJsonValue(child, json);
                            }
                        }
                    }
                }
            }
        }
    }

    public JSONObject getCommonFieldsNgDynamicForms() {
        JSONObject field = new JSONObject();

        String label = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(name), " ");

        field.put("cls", generateNgDynamicFormsCls());

        field.put("id",          name);
        field.put("label",       label);
        field.put("placeholder", label);
        field.put("type",        getNgDynamicFormsControlType());
        field.put("required",    !isOptional());

        readAppInfoDynamicForms(field);

        return field;
    }

    public JSONObject generateNgDynamicForms() {
        JSONObject input = getCommonFieldsNgDynamicForms();

        input.put("inputType", "text");

        return input;
    }
}
