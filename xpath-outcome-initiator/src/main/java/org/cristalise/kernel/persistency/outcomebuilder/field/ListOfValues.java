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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.Documentation;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.SimpleType;

public class ListOfValues extends HashMap<String, Object> {
    
    public enum AppInfoListTags {scriptRef, propertyNames, queryRef, inputName};

    private static final long serialVersionUID = -2718359690741674876L;

    String            defaultKey  = null;
    ArrayList<String> orderedKeys = new ArrayList<String>();

    public ListOfValues() {
        super();
    }

    public String put(String key, Object value, boolean isDefaultKey) {
        if (isDefaultKey) defaultKey = key;
        orderedKeys.add(key);
        return (String) super.put(key, value);
    }

    public String[] getKeyArray() {
        return orderedKeys.toArray(new String[orderedKeys.size()]);
    }

    public String getDefaultKey() {
        return defaultKey;
    }

    public void setDefaultValue(String newDefaultVal) {
        defaultKey = findKey(newDefaultVal);
    }

    public String findKey(String value) {
        for (String key : keySet()) {
            if (get(key).equals(value)) return key;
        }
        return null;
    }

    public Object getDefaultValue() {
        return get(defaultKey);
    }

    protected void createLOV(SimpleType contentType, AnyNode listNode) {
        if (listNode != null) { // schema instructions for list building
            String lovType = listNode.getLocalName();
            String param = listNode.getFirstChild().getStringValue();

            if      (lovType.equals("scriptList")) populateLOVFromScript(param);
            else if (lovType.equals("pathList"))   populateLOVFromLookup(param);
            else if (lovType.equals("queryList"))  ; //populateLOVFromQuery(param);
            else if (lovType.equals("valueList"))  ; //populateLOVFromValues(param);
        }

        // handle enumerations
        // TODO: should be merged with above results
        if (contentType.hasFacet(Facet.ENUMERATION)) {
            //ListOfValues andList = new ListOfValues();
            Enumeration<Facet> enums = contentType.getFacets(Facet.ENUMERATION);
            //TODO: read default value if exists

            while (enums.hasMoreElements()) {
                Facet thisEnum = enums.nextElement();
                String desc = thisEnum.getValue();
                Enumeration<Annotation> annos = thisEnum.getAnnotations();

                if (annos.hasMoreElements()) {
                    Annotation thisAnno = annos.nextElement();
                    Enumeration<Documentation> docs = thisAnno.getDocumentation();

                    if (docs.hasMoreElements()) desc = docs.nextElement().getContent();
                }

                put(desc, thisEnum.getValue(), false);
            }
        }
    }

    /**
     * @param param
     */
    private void populateLOVFromLookup(String param) {
        // TODO List of Values from Lookup properties, eg '/root/path;prop=val;prop=val'
    }

    private void populateLOVFromScript(String scriptName) {
        try {
            StringTokenizer tok = new StringTokenizer(scriptName, "_");

            if (tok.countTokens() != 2) throw new Exception("Invalid LOVScript name");

            Script lovscript = LocalObjectLoader.getScript(tok.nextToken(), Integer.parseInt(tok.nextToken()));
            lovscript.setInputParamValue("LOV", null/*vals*/);
            lovscript.execute();
        }
        catch (Exception ex) {
            Logger.error(ex);
        }
    }

}
