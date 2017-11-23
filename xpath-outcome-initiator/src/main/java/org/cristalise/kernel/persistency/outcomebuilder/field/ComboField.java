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

import java.util.Enumeration;
import java.util.StringTokenizer;

import org.cristalise.kernel.persistency.outcomebuilder.StructuralException;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.AttributeDecl;
import org.exolab.castor.xml.schema.Documentation;
import org.exolab.castor.xml.schema.ElementDecl;
import org.exolab.castor.xml.schema.Facet;
import org.exolab.castor.xml.schema.SimpleType;

public class ComboField extends StringField {

    ListOfValues vals;
    AnyNode listNode;
    String selected;

    public ComboField(SimpleType type, AnyNode listNode) {
        super();
        content = type;
        this.listNode = listNode;
        createLOV();
    }

    @Override
    public String getDefaultValue() {
        if (vals.getDefaultKey() != null)
            return vals.get(vals.getDefaultKey()).toString();
        else
            return "";
    }

    public void setDefaultValue(String defaultVal) {
        vals.setDefaultValue(defaultVal);
    }

    @Override
    public String getText() {
        return vals.get(selected).toString();
    }

    @Override
    public void setText(String text) {
        if (vals.containsValue(text)) {
            selected = vals.findKey(text);
        }
        else
            Logger.error("Illegal value for ComboField name:'"+getName()+"' value:'"+text+"'");
    }

    private void createLOV() {
        vals = new ListOfValues();

        if (listNode != null) { // schema instructions for list building
            String lovType = listNode.getLocalName();
            String param = listNode.getFirstChild().getStringValue();

            if (lovType.equals("ScriptList")) populateLOVFromScript(param);
            if (lovType.equals("PathList"))   populateLOVFromLookup(param);
        }

        // handle enumerations
        // TODO: should be merged with above results
        if (content.hasFacet(Facet.ENUMERATION)) {
            //ListOfValues andList = new ListOfValues();
            Enumeration<Facet> enums = content.getFacets(Facet.ENUMERATION);
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
                vals.put(desc, thisEnum.getValue(), false);
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
            lovscript.setInputParamValue("LOV", vals);
            lovscript.execute();
        }
        catch (Exception ex) {
            Logger.error(ex);
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
}