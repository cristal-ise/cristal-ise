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

import org.json.JSONObject;

public class LongStringField extends StringField {

    public LongStringField() {
        super();
    }

    @Override
    public String getNgDynamicFormsControlType() {
        return "TEXTAREA";
    }

    @Override
    public JSONObject generateNgDynamicForms(Map<String, Object> inputs, boolean withModel, boolean withLayout) {
        JSONObject textArea = getNgDynamicFormsCommonFields(withModel, withLayout);

        textArea.put("cols", 20);
        textArea.put("rows", 5);
        textArea.put("wrap", "soft");
        textArea.put("autocomplete", "on");

        return textArea;
    }
}
