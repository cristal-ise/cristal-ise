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
    Structure  model;
    boolean    isValid  = true;
    String     name;
    SimpleType content;
    String     field;
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
        if (!(type.isBuiltInType())) type = type.getBuiltInBaseType();

        // else derive the class
        Class<?> contentClass = OutcomeStructure.getJavaClass(type.getTypeCode());

        // disable list edits for the moment
        if (contentClass.equals(Boolean.class))            return new BooleanField();
        else if (contentClass.equals(BigInteger.class))    return new IntegerField();
        else if (contentClass.equals(BigDecimal.class))    return new DecimalField();
        else if (contentClass.equals(LocalDate.class))     return new DateField();
        else if (contentClass.equals(OffsetTime.class))    return new TimeField();
        else if (contentClass.equals(OffsetDateTime.class))return new DateTimeField();
        //else if (contentClass.equals(ImageIcon.class)) return new ImageEditField();
        else if (length > 60)                              return new LongStringField();
        else                                               return new StringField();
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

    public void setDecl(AttributeDecl model) throws StructuralException {
        this.model = model;
        this.content = model.getSimpleType();
        this.name = model.getName();
        defaultValue = model.getDefaultValue();
    }

    public void setDecl(ElementDecl model) throws StructuralException {
        this.model = model;
        this.name = model.getName();
        XMLType type = model.getType();
        defaultValue = model.getDefaultValue();

        // derive base type
        if (type.isSimpleType()) this.content = (SimpleType) type;
        else                     this.content = (SimpleType) (type.getBaseType());

        if (this.content == null) throw new StructuralException("No declared base type of element");
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

    public Structure getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public Node getData() {
        return data;
    }

    public String getDefaultValue() {
        return "";
    }

    public void updateNode() {
        if (data == null) return;

        if (data instanceof Text) ((Text) data).setData(getText());
        else                      ((Attr) data).setValue(getText());
    }

    public String getText() {
        return field;
    }

    public void setText(String text) {
        field = text;
    }

    public String getNgDynamicFormsControlType() {
        return "INPUT";
    }

    public JSONObject getCommonFieldsNgDynamicForms() {
        JSONObject field = new JSONObject();

        // label could be specified in appInfo
        String label = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(name), " ");

        field.put("id",          name);
        field.put("label",       label);
        field.put("placeholder", label);
        field.put("type",        getNgDynamicFormsControlType());
        
        String defVal = getDefaultValue();
        
        

        return field;
    }

    public JSONObject generateNgDynamicForms() {
        JSONObject input = getCommonFieldsNgDynamicForms();

        input.put("inputType", "text");

        return input;
    }
}
