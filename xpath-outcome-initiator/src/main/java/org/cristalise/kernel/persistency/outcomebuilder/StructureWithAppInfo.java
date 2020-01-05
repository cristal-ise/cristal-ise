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
package org.cristalise.kernel.persistency.outcomebuilder;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;

import org.exolab.castor.types.AnyNode;
import org.exolab.castor.xml.schema.Annotated;
import org.exolab.castor.xml.schema.Annotation;
import org.exolab.castor.xml.schema.AppInfo;
import org.json.JSONObject;
import org.json.XML;

public class StructureWithAppInfo {

    /**
     * List of field names might contain string which will be recognized by Scanner a numeric type. 
     * (Scanner is locale specific as well)
     */
    protected List<String> stringFields;

    /**
     * List of field names which are processed individually by the specific subclasses
     */
    protected List<String> exceptionFields;

    /**
     * 
     */
    protected JSONObject additional = null;

    public StructureWithAppInfo() {
        stringFields = new ArrayList<String>();
        exceptionFields = new ArrayList<String>();
    }

    public StructureWithAppInfo(List<String> strFields, List<String> excFields) {
        stringFields = new ArrayList<String>(strFields);
        exceptionFields = new ArrayList<String>(excFields);
    }

    /**
     * Check if the value contains a template/pattern that can be interpreted by the given Field instance
     * 
     * @param valueTemplate
     * @return
     */
    public String getValue(String valueTemplate) {
        return valueTemplate;
    }

    /**
     * To be overridden by subclasses to handle data locally available in AppInfo.DynamicForms
     * 
     * @param name the name of the field
     * @param value the value of the field
     */
    protected void setAppInfoDynamicFormsExceptionValue(String name, String value) {
    }

    /**
     * 
     * @param node
     * @param json
     */
    protected void setAppInfoDynamicFormsJsonValue(AnyNode node, JSONObject json, Boolean formGridCls) {
        String name  = node.getLocalName();

        if (name.equals("additional")) {
            //simply convert the xml to json and store it
            additional = XML.toJSONObject(node.toString(), false).getJSONObject("additional");
        }
        else {
            String value = node.getStringValue().trim();
            if (name.equals("value")) value = getValue(value);

            if (stringFields.contains(name)) {
                json.put(name, value);
            }
            else if (formGridCls) {
                if (name.equals("container")) json.put(name, value);
                else                          return;
            }
            else if (exceptionFields.contains(name)) {
                setAppInfoDynamicFormsExceptionValue(name, value);
            }
            else {
                Scanner scanner = new Scanner(value);

                if      (scanner.hasNextBoolean())    json.put(name, scanner.nextBoolean());
                else if (scanner.hasNextBigDecimal()) json.put(name, scanner.nextBigDecimal());
                else if (scanner.hasNextBigInteger()) json.put(name, scanner.nextBigInteger());
                else if (!name.equals("container"))   json.put(name, value);

                scanner.close();
            }
        }
    }

    /**
     * Finds the named element in the AppInfo node
     * 
     * @param aModel the schema model to search
     * @param name the name of the element in the AppInfo node
     * @return the AnyNode with the given name otherwise null
     */
    public static AnyNode getAppInfoNode(Annotated  aModel, String name) {
        Enumeration<Annotation> e = aModel.getAnnotations();

        while (e.hasMoreElements()) {
            Annotation note = e.nextElement();

            for (Enumeration<AppInfo> f = note.getAppInfo(); f.hasMoreElements();) {
                AppInfo thisAppInfo = f.nextElement();

                for (Enumeration<?> g = thisAppInfo.getObjects(); g.hasMoreElements();) {
                    AnyNode appInfoNode = (AnyNode) g.nextElement();

                    if (appInfoNode.getNodeType() == AnyNode.ELEMENT && name.equals(appInfoNode.getLocalName())) {
                        return appInfoNode;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 
     * @param aModel
     * @param json
     */
    public void readAppInfoDynamicForms(Annotated aModel, JSONObject json, Boolean formGridCls) {
        AnyNode appInfoNode = getAppInfoNode(aModel, "dynamicForms");

        if (appInfoNode != null) {
            AnyNode child = appInfoNode.getFirstChild(); //stupid API, there is no getChildren

            if (child != null) {
                if (child.getNodeType() == AnyNode.ELEMENT) setAppInfoDynamicFormsJsonValue(child, json, formGridCls);

                for (child = child.getNextSibling(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == AnyNode.ELEMENT) setAppInfoDynamicFormsJsonValue(child, json, formGridCls);
                }
            }
        }
    }
}
