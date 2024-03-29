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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.exolab.castor.xml.schema.Facet;
import org.json.JSONObject;

public abstract class NumberField extends StringField {
    
    /**
     * For integers or decimals
     */
    String precision;
    
    public NumberField() {
        super();
    }

    public NumberField(List<String> strFields, List<String> excFields) {
        super(strFields, excFields);
    }

    @Override
    public String getNgDynamicFormsControlType() {
        return "INPUT";
    }
    
    protected BigDecimal getRangeValue(String facetName) {
        Enumeration<Facet> facets = contentType.getFacets(facetName);

        while (facets.hasMoreElements()) {
            Facet nextFacets = facets.nextElement();
            return new BigDecimal(nextFacets.getValue());
        }

        return null;
    }

    @Override
    public JSONObject generateNgDynamicForms(Map<String, Object> inputs, boolean withModel, boolean withLayout) {
        JSONObject inputReal = getNgDynamicFormsCommonFields(withModel, withLayout);

        //inputReal.put("inputType", "number");

        return inputReal;
    }

    @Override
    public void setNgDynamicFormsValidators(JSONObject validators) {
        if (contentType.hasFacet(Facet.MIN_EXCLUSIVE) || contentType.hasFacet(Facet.MIN_INCLUSIVE)) {
            BigDecimal minValue = getRangeValue(Facet.MIN_INCLUSIVE);

            if (minValue == null) minValue = getRangeValue(Facet.MIN_EXCLUSIVE);
            
            if (minValue != null) validators.put("min", minValue);
        }

        if (contentType.hasFacet(Facet.MAX_EXCLUSIVE) || contentType.hasFacet(Facet.MAX_INCLUSIVE)) {
            BigDecimal maxValue = getRangeValue(Facet.MAX_INCLUSIVE);

            if (maxValue == null) maxValue = getRangeValue(Facet.MAX_EXCLUSIVE);

            if (maxValue != null) validators.put("max", maxValue);
        }

        if (StringUtils.isNotBlank(pattern)) {
            validators.put("pattern", pattern);
        } 
    }

    @Override
    public boolean hasValidator() {
        return super.hasValidator() || StringUtils.isNotBlank(precision);
    }
}
