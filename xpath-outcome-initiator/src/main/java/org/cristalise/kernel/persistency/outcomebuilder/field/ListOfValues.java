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

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.Documentation;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.SimpleType;

public class ListOfValues extends HashMap<String, Object> {
    
    public enum AppInfoListTags {scriptRef, propertyNames, queryRef, inputName, values};

    private static final long serialVersionUID = -2718359690741674876L;

    SimpleType        contentType;
    AnyNode           listNode;
    String            defaultKey  = null;
    ArrayList<String> orderedKeys = new ArrayList<String>();

    boolean editable = false;

    public ListOfValues(SimpleType type, AnyNode list) {
        super();
        contentType = type;
        listNode = list;

        populateLovFromEnumeration();
    }

    public String put(String key, Object value, boolean isDefaultKey) {
        if (isDefaultKey) defaultKey = key;

        if (!orderedKeys.contains(key)) orderedKeys.add(key);

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

    private void callLovPopulate(AnyNode lovNode, Map<String, Object> inputs) {
        String lovType = lovNode.getLocalName();

        Logger.msg(5, "ListOfValues.callLovPopulate() - lovType:"+lovType);

        AnyNode param = lovNode.getFirstChild();

        switch (AppInfoListTags.valueOf(lovType)) {
            case inputName:     populateLOVFromInput(param, inputs); break;
            case propertyNames: populateLOVFromLookup(param, inputs); break;
            case queryRef:      populateLOVFromQuery(param, inputs); break; 
            case scriptRef:     populateLOVFromScript(param, inputs); break;
            case values:        populateLOVFromValues(param); break;

            default:
                Logger.warning("ListOfValues.callLovPoupulate() - unhandled type:"+lovType);
        }
    }

    protected void createLOV(Map<String, Object> inputs) {
        if (listNode != null) { // schema instructions for list building
            AnyNode child = listNode.getFirstChild(); //stupid API, there is no getChildren

            if (child != null) {
                if (child.getNodeType() == AnyNode.ELEMENT) callLovPopulate(child, inputs);

                for (child = child.getNextSibling(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == AnyNode.ELEMENT) callLovPopulate(child, inputs);
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

        if (inputs != null && inputs.containsKey(key)) {
            try {
                Object val = inputs.get(key);
                if (val instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> values = (List<String>)val;

                    if (values.size() > 0) {
                        clear();
                        for (String value : values) put(value, value, false);
                    }
                    else 
                        Logger.warning("ListOfValues.populateLOVFromInput() - NO Inputs were found for param:"+key);
                }
                else if(val instanceof String) {
                    String values = (String)val;
                    
                    if (StringUtils.isNotBlank(values)) {
                        clear();
                        for(String value: values.split(",")) put(value, value, false);
                    }
                    else 
                        Logger.warning("ListOfValues.populateLOVFromInput() - NO Inputs were found for param:"+key);
                }
                else if(val instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> values = (Map<String, Object>)val;

                    if ( ! values.isEmpty() ) {
                        clear();
                        extractValues(values, true);
                    }
                    else {
                        Logger.warning("ListOfValues.populateLOVFromInput() - NO Inputs were found for param:"+key);
                    }
                }
            }
            catch (Exception e) {
                Logger.error(e);
            }
        }
        else 
            Logger.warning("ListOfValues.populateLOVFromInput() - NO Inputs were found param:"+key);
    }
    
    /**
     * 
     * @param values
     */
    @SuppressWarnings("unchecked")
    private void extractValues(Map<String, Object> values, boolean switchKeyAndValue) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() instanceof Map) {
                extractValues((Map<String, Object>) entry.getValue(), switchKeyAndValue);
            }else {
                if (switchKeyAndValue) {
                    put(entry.getValue().toString(), entry.getKey(), false);
                }
                else {
                    put(entry.getKey(), entry.getValue().toString(), false);
                }
            }
        }
    }

    private void populateLOVFromValues(AnyNode paramNode) {
        if (paramNode.getNodeType() != AnyNode.TEXT) {
            Logger.warning("ListOfValues.populateLOVFromValues() - paramNode is not a TEXT");
            return;
        }

        for (String value: paramNode.getStringValue().split(",")) put(value, value, false);

        editable = true;
    }

    private void populateLOVFromLookup(AnyNode param, Map<String, Object> inputs) {
        assert false;
    }

    private void populateLOVFromQuery(AnyNode queryRef, Map<String, Object> inputs) {
        assert false;
    }

    @SuppressWarnings("unchecked")
    private void populateLOVFromScript(AnyNode scriptRefNode, Map<String, Object> inputs) {
        if (scriptRefNode.getNodeType() != AnyNode.TEXT) {
            Logger.warning("ListOfValues.populateLOVFromScript() - AnyNode is not a TEXT");
            return;
        }

        String[] scriptRefTokens = scriptRefNode.getStringValue().split(":");
        
        try {
            if (scriptRefTokens.length != 2) {
                Logger.error("populateLOVFromScript; Invalid LOVScript name: " + scriptRefNode.getStringValue());
                throw new InvalidDataException("Invalid LOVScript name");
            }
            Script script = LocalObjectLoader.getScript(scriptRefTokens[0], Integer.valueOf(scriptRefTokens[1]));

            //TODO: set input parameters in the CastorHashMap
            Map<? extends String, ? extends Object> result = (Map<? extends String, ? extends Object>) 
                        script.evaluate(null, new CastorHashMap(), null, null);

            extractValues((Map<String, Object>) result, false);
        }
        catch (NumberFormatException | ObjectNotFoundException | InvalidDataException | ScriptingEngineException e) {
            Logger.error(e);
        }
    }

}
