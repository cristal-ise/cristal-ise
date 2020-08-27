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

import static org.exolab.castor.xml.schema.Facet.ENUMERATION;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.persistency.outcomebuilder.InvalidOutcomeException;
import org.cristalise.kernel.persistency.outcomebuilder.StructuralException;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.SimpleType;
import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ComboField extends StringField {

    ListOfValues lov = null;
    List<String> selecteds = new ArrayList<>();
    boolean isMultiple = false;

    public ComboField(SimpleType type, AnyNode listNode, boolean multiple) {
        super();
        contentType = type;

        if (contentType.hasFacet(ENUMERATION) || listNode != null) {
            lov = new ListOfValues(type, listNode);
        }

        //TODO: perhaps this case it should be covered by a new Field class, i.e. ChipsField
        // if (lov == null && !multiple) throw new RuntimeException("");

        isMultiple = multiple;
    }

    @Override
    public String getDefaultValue() {
        if (lov == null) return "";

        if (lov.getDefaultKey() != null) return lov.get(lov.getDefaultKey()).toString();
        else                             return "";
    }

    public void setDefaultValue(String defaultVal) {
        if (lov != null) lov.setDefaultValue(defaultVal);
    }

    @Override
    public String getText() {
        return StringUtils.join(selecteds, ",");
    }
    
    protected void setSelected(String value) {
        if (isMultiple || selecteds.size() == 0) {
            selecteds.add(value);
        }
        else {
            selecteds.set(0, value);
        }
    }

    @Override
    public void setText(String text) {
        if (lov != null && lov.strictValueHandling) {
            if (lov.containsValue(text)) {
                setSelected(lov.findKey(text));
            }
            else
                log.error("Illegal value for ComboField name:'"+getName()+"' value:'"+text+"'");
        }
        else {
            setSelected(text);
        }
    }

    @Override
    public void setDecl(AttributeDecl model) throws StructuralException {
        super.setDecl(model);
        setDefaultValue(model.getDefaultValue());
    }

    @Override
    public void setDecl(ElementDecl model) throws StructuralException {
        super.setDecl(model);
        setDefaultValue(model.getDefaultValue());
    }

    @Override
    public String getNgDynamicFormsControlType() {
        return "SELECT";
    }

    private JSONArray getNgDynamicFormsOptions() {
        JSONArray options = new JSONArray();

        if (lov != null && lov.size() != 0) {
            JSONObject emptyOption = new JSONObject();
            emptyOption.put("label", "Select value");

            options.put(emptyOption);

            for (String key: lov.orderedKeys) {
                if (lov.get(key) != null) {
                    JSONObject option = new JSONObject();

                    option.put("label", key);
                    option.put("value", lov.get(key));

                    options.put(option);
                }
            }
        }

        return options;
    }

    @Override
    public JSONObject generateNgDynamicForms(Map<String, Object> inputs) {
        if (lov == null) return null;

        lov.createLOV(inputs);

        JSONObject select = getCommonFieldsNgDynamicForms();

        JSONArray options = getNgDynamicFormsOptions();

        if (options.length() != 0) {
            select.put("options", options);

            JSONObject additional = getAdditionalConfigNgDynamicForms(select);
 
            if (lov.editable) {
                additional.put("editable", true);
            }

            select.put("filterable", true);
            additional.put("filterBy", "label");
        }
        else{
            select.put("type", "INPUT"); // overwrite type if no values were given
            select.put("disabled", true); // also disable it, as it is very likely an error
        }

        return select;
    }

    @Override
    public void setValue(Object value) throws InvalidOutcomeException {
        super.setValue(value);
    }
}