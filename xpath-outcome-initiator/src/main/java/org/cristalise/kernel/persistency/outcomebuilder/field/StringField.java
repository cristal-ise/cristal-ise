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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.persistency.outcomebuilder.InvalidOutcomeException;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeStructure;
import org.cristalise.kernel.persistency.outcomebuilder.StructuralException;
import org.cristalise.kernel.persistency.outcomebuilder.StructureWithAppInfo;
import org.cristalise.kernel.process.Gateway;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotated;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.SimpleType;
import org.exolab.castor.xml.schema.SimpleTypesFactory;
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
public class StringField extends StructureWithAppInfo {

    private static final String[] strFields = {"mask", "placeholder"};
    private static final String[] excFields = {"pattern", "errmsg","container", "control", "labelGrid"};

    Node       data;
    Annotated  model;
    boolean    isValid  = true;
    boolean    isAttribute = false;
    String     name;
    SimpleType contentType;
    String     text;
    String     defaultValue;
    
    String     container;
    String     control;
    String     labelGrid;
    
    /**
     * Javascript regexp pattern to validate field value in DynamicForms. It is either provided in the AppInfo.pattern field 
     * or it is computed from data available in XSD restrictions or in various AppInfo fields (check subclasses)
     */
    String pattern;
    /**
     * Error message to show to the user for validation errors
     */
    String errmsg;

    public StringField() {
        this(Arrays.asList(strFields), Arrays.asList(excFields));
    }

    public StringField(List<String> strFields, List<String> excFields) {
        super(strFields, excFields);
    }

    /**
     * 
     * @param model
     * @return
     */
    public static SimpleType getFieldType(Annotated  model) {
        if (model instanceof ElementDecl) {
            XMLType baseType = ((ElementDecl)model).getType();

            while (!(baseType instanceof SimpleType)) baseType = baseType.getBaseType();

            return (SimpleType)baseType;
        }
        else if (model instanceof AttributeDecl) {
            return ((AttributeDecl)model).getSimpleType();
        }

        return null;
    }

    /**
     * 
     * @param model
     * @return
     */
    private static StringField getFieldForType(Annotated  model) {
        SimpleType type = getFieldType(model);

        // handle lists special
        if (type instanceof ListType) return new ArrayField(type.getBuiltInBaseType());

        // is a combobox
        AnyNode appInfoNode = StructureWithAppInfo.getAppInfoNode(model, "listOfValues");
        if (type.hasFacet(Facet.ENUMERATION) || appInfoNode != null) return new ComboField(type, appInfoNode);

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

        StringField newField = getFieldForType(model);
        newField.setDecl(model);

        return newField;
    }

    public static StringField getField(ElementDecl model) throws StructuralException {
        try {
            StringField newField = getFieldForType(model);

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
        if (data == null) throw new InvalidOutcomeException("TextNode for '"+name+ "' does not exists");
        setText(newData);
        updateNode();
    }

    /**
     * Overwrite this method to provide conversion code for each type
     * 
     * @param value the Object to be converted
     * @throws InvalidOutcomeException
     */
    public void setValue(Object value) throws InvalidOutcomeException {
        setData(value.toString());
    }

    public boolean isOptional() {
        if (isAttribute) return ((AttributeDecl)model).isOptional();
        else             return ((ElementDecl)model).getMinOccurs() == 0;
    }

    public boolean hasValidator() {
        return  contentType.hasFacet(Facet.MIN_EXCLUSIVE)
             || contentType.hasFacet(Facet.MAX_EXCLUSIVE)
             || contentType.hasFacet(Facet.MIN_INCLUSIVE)
             || contentType.hasFacet(Facet.MAX_INCLUSIVE)
             || contentType.hasFacet(Facet.MIN_LENGTH)
             || contentType.hasFacet(Facet.MAX_LENGTH)
             || contentType.hasFacet(Facet.LENGTH)
             || contentType.hasFacet(Facet.TOTALDIGITS)
             || contentType.hasFacet(Facet.FRACTIONDIGITS)
             || getFieldType(model).getTypeCode() == SimpleTypesFactory.DECIMAL_TYPE //always generated for decimal field
             || StringUtils.isNotBlank(pattern);
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

    public void setNgDynamicFormsValidators(JSONObject validators) {
        if (StringUtils.isNotBlank(pattern)) {
            validators.put("pattern", pattern);
        }

        if(contentType.hasFacet(Facet.MIN_LENGTH)) {
            Facet minLength = contentType.getFacet(Facet.MIN_LENGTH);
            validators.put(minLength.getName(), minLength.getValue());
        }

        if(contentType.hasFacet(Facet.MAX_LENGTH)) {
            Facet maxLength = contentType.getFacet(Facet.MAX_LENGTH);
            validators.put(maxLength.getName(), maxLength.getValue());
        }

        if(contentType.hasFacet(Facet.LENGTH)) {
            Facet length = contentType.getFacet(Facet.LENGTH);
            validators.put("minLength", length.getValue());
            validators.put("maxLength", length.getValue());
        }
    }

    public void setNgDynamicFormsErrorMessages(JSONObject errorMessages) {
        if (StringUtils.isNotBlank(errmsg)) {
            errorMessages.put("pattern", errmsg);
        }
    }

    public JSONObject generateNgDynamicFormsCls() {
        JSONObject fieldCls = new JSONObject();

        JSONObject fieldElement = new JSONObject();
        fieldElement.put("label", "ui-widget");

        JSONObject fieldGrid = new JSONObject();
        fieldGrid.put("container", StringUtils.isNotBlank(container) ? container : "ui-g");
        
        // If either the control or the label is not defined, both are put to their default values
        if (!StringUtils.isNotBlank(labelGrid) || !StringUtils.isNotBlank(control)) {
           labelGrid = "ui-g-4";
           control = "ui-g-8";
        }
        
        fieldGrid.put("label",     labelGrid);
        fieldGrid.put("control",   control);

        fieldCls.put("element", fieldElement);
        fieldCls.put("grid", fieldGrid);
        return fieldCls;
    }
    
    @Override
    protected void setAppInfoDynamicFormsExceptionValue(String name, String value) {
        if (name.equals("pattern")) {
            pattern = value;
        }
        else if (name.equals("errmsg")) {
            errmsg = value;
        }
        else if (name.equals("container")) {
            container = value;
        }
        else if (name.equals("control")) {
            control = value;
        }
        else if (name.equals("labelGrid")) {
            labelGrid = value;
        }
    }
    
    /**
     * PrimeNG specific settings are stored in 'additional' JSONOBject of NgDynamicForms configs
     * 
     * @param field the actual config field 
     * @return the 'additional' JSONOBject attached to the config field
     */
    public JSONObject getAdditionalConfigNgDynamicForms(JSONObject field) {
        if (field.has("additional")) {
            return (JSONObject) field.get("additional");
        }
        else {
            JSONObject additional = new JSONObject();
            field.put("additional", additional);
            return additional;
        }
    }

    public JSONObject getCommonFieldsNgDynamicForms() {
        JSONObject field = new JSONObject();
        
        field.put("id",       name);
        field.put("label",    name);
        field.put("type",     getNgDynamicFormsControlType());
        field.put("required", !isOptional());

        //This can overwrite values set earlier, for example 'type' can be changed from INPUT to RATING
        readAppInfoDynamicForms(model, field, false);

        field.put("cls", generateNgDynamicFormsCls());

        JSONObject validators = new JSONObject();
        field.put("validators", validators);

        JSONObject errorMessages = new JSONObject();
        field.put("errorMessages", errorMessages);

        boolean required = field.getBoolean("required");

        if (!isOptional() && required) validators.put("required", JSONObject.NULL);

        if (hasValidator()) {
            setNgDynamicFormsValidators(validators);
            setNgDynamicFormsErrorMessages(errorMessages);
        }

        // appinfo/dynamicForms could have updated label, so do the CamelCase splitting now
        String label = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase((String)field.get("label")), " ");
        label.replaceAll(" *", " ");
        field.put("label", label + (required ? " *": ""));

        //Put label as placholder if it was not specified in the Schema
        if (! field.has("placeholder")) field.put("placeholder", label);

        String defaultAutoComplete = Gateway.getProperties().getString("Webui.autoComplete.default", "off");

        // autoComplete=on by default in NgDyanmicForms so no need to set
        if (! field.has("autoComplete") && defaultAutoComplete.equals("off") ) {
            field.put("autoComplete", defaultAutoComplete);
        }

        // if validators has no elements then remove it.
        if (field.getJSONObject("validators").length() == 0) {
            field.remove("validators");
        }
        // if errorMessages has no elements then remove it.
        if (field.getJSONObject("errorMessages").length() == 0) {
            field.remove("errorMessages");
        }

        return field;
    }

    public JSONObject generateNgDynamicForms(Map<String, Object> inputs) {
        JSONObject input = getCommonFieldsNgDynamicForms();

        // AppInfo could set the 'inputType' to password already
        if (!input.has("inputType")) input.put("inputType", "text");

        return input;
    }
}
