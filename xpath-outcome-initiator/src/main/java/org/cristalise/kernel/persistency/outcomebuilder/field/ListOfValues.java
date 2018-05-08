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
import java.util.List;
import java.util.Map;

import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.Documentation;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.SimpleType;

public class ListOfValues extends HashMap<String, Object> {
    
    public enum AppInfoListTags {scriptRef, propertyNames, queryRef, inputName};

    private static final long serialVersionUID = -2718359690741674876L;

    SimpleType        contentType;
    AnyNode           listNode;
    String            defaultKey  = null;
    ArrayList<String> orderedKeys = new ArrayList<String>();

    public ListOfValues(SimpleType type, AnyNode list) {
        super();
        contentType = type;
        listNode = list;
        
        populateLovFromEnumeration();
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

    private void populateLovFromEnumeration() {
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

    private void callLovPoupulate(AnyNode lovNode, Map<String, Object> inputs) {
        String lovType = lovNode.getLocalName();

        Logger.msg(5, "ListOfValues.callLovPoupulate() - lovType:"+lovType);

        AnyNode param = lovNode.getFirstChild();

        switch (AppInfoListTags.valueOf(lovType)) {
            case inputName:     populateLOVFromInput(param, inputs); break;
            case propertyNames: populateLOVFromLookup(param, inputs); break;
            case queryRef:      populateLOVFromQuery(param, inputs); break; 
            case scriptRef:     populateLOVFromScript(param, inputs); break;

            default:
                Logger.warning("ListOfValues.callLovPoupulate() - unhadled type:"+lovType);
        }
    }

    protected void createLOV(Map<String, Object> inputs) {
        if (listNode != null) { // schema instructions for list building
            AnyNode child = listNode.getFirstChild(); //stupid API, there is no getChildren

            if (child != null) {
                if (child.getNodeType() == AnyNode.ELEMENT) callLovPoupulate(child, inputs);

                for (child = child.getNextSibling(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == AnyNode.ELEMENT) callLovPoupulate(child, inputs);
                }
            }
        }
        // handle enumerations
        // TODO: should be merged with above results
        // check method populateLovFromEnumeration();
    }

    private void populateLOVFromInput(AnyNode paramNode, Map<String, Object> inputs) {
        if (paramNode.getNodeType() != AnyNode.TEXT) {
            Logger.warning("ListOfValues.populateLOVFromInput() - paramNode is not a TEXT");
            return;
        }

        String key = paramNode.getStringValue();

        if (inputs.containsKey(key)) {
            try {
                @SuppressWarnings("unchecked")
                List<String> values = (List<String>)inputs.get(key);

                if (values.size() > 0) {
                    for (String value : values) put(value, value, false);
                }
                else 
                    Logger.warning("ListOfValues.populateLOVFromInput() - NO Inputs were found for param:"+key);
            }
            catch (Exception e) {
                Logger.error(e);
            }
        }
        else 
            Logger.warning("ListOfValues.populateLOVFromInput() - NO Inputs were found for param:"+key);
    }

    /**
     * @param param
     */
    private void populateLOVFromLookup(AnyNode param, Map<String, Object> inputs) {
        assert false;
    }

    private void populateLOVFromQuery(AnyNode scriptName, Map<String, Object> inputs) {
        assert false;
    }

    private void populateLOVFromScript(AnyNode scriptName, Map<String, Object> inputs) {
        assert false;
        /*
        try {
            StringTokenizer tok = new StringTokenizer(scriptName, "_");

            if (tok.countTokens() != 2) throw new Exception("Invalid LOVScript name");

            Script lovscript = LocalObjectLoader.getScript(tok.nextToken(), Integer.parseInt(tok.nextToken()));
            lovscript.setInputParamValue("LOV", vals);
            lovscript.execute();
        }
        catch (Exception ex) {
            Logger.error(ex);
        }
        */
    }

}
