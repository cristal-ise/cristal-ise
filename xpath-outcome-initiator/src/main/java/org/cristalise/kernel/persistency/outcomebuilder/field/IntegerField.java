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

import static org.cristalise.kernel.persistency.outcomebuilder.SystemProperties.Webui_inputField_integer_defaultValue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

import org.json.JSONObject;

public class IntegerField extends NumberField {

    private static final String[] strFields = {"mask", "placeholder"};
    private static final String[] excFields = {"pattern", "errmsg", "precision"};

    public IntegerField() {
        super(Arrays.asList(strFields), Arrays.asList(excFields));
        javaType = BigInteger.class;
    }

    @Override
    public String getDefaultValue() {
        return Webui_inputField_integer_defaultValue.getString();
    }

    @Override
    public JSONObject generateNgDynamicForms(Map<String, Object> inputs, boolean withModel, boolean withLayout) {
        JSONObject inputInt = super.generateNgDynamicForms(inputs, withModel, withLayout);

        if (inputInt.getString("type").equals("RATING")) {
            // disable cancel icon for RATING
            getNgDynamicFormsAdditional(inputInt).put("cancel", false);
        }

        return inputInt;
    }
}
