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

import java.util.Map;

import org.cristalise.kernel.persistency.outcomebuilder.StructuralException;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.SimpleType;
import org.json.JSONArray;
import org.json.JSONObject;

public class ComboField extends StringField {

    ListOfValues vals;
    String selected;
    
    // when values are defined in schema enumeration, check the values against that list
    boolean strictValueHandling;

    public ComboField(SimpleType type, AnyNode listNode) {
        super();
        contentType = type;
        strictValueHandling = listNode == null;
        vals = new ListOfValues(type, listNode);
    }

    @Override
    public String getDefaultValue() {
        if (vals.getDefaultKey() != null) return vals.get(vals.getDefaultKey()).toString();
        else                              return "";
    }

    public void setDefaultValue(String defaultVal) {
        vals.setDefaultValue(defaultVal);
    }

    @Override
    public String getText() {
        if (strictValueHandling) return selected != null ? vals.get(selected).toString() : "";
        else                     return selected;
    }

    @Override
    public void setText(String text) {
        if (strictValueHandling) {
            if (vals.containsValue(text)) {
                selected = vals.findKey(text);
            }
            else
                Logger.error("Illegal value for ComboField name:'"+getName()+"' value:'"+text+"'");
        }
        else
            selected = text;
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

        if (vals.size() != 0) {
            JSONObject emptyOption = new JSONObject();
            emptyOption.put("label", "Select value");

            options.put(emptyOption);

            for (String key: vals.orderedKeys) {
                if (vals.get(key) != null) {
                    JSONObject option = new JSONObject();

                    option.put("label", key);
                    option.put("value", vals.get(key));

                    options.put(option);
                }
            }
        }

        return options;
    }

    @Override
    public JSONObject generateNgDynamicForms(Map<String, Object> inputs) {
        vals.createLOV(inputs);

        JSONObject select = getCommonFieldsNgDynamicForms();

        JSONArray options = getNgDynamicFormsOptions();

        if (options.length() != 0) {
            select.put("options", options);

            JSONObject additional = getAdditionalConfigNgDynamicForms(select);
 
            if (vals.editable) {
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
}