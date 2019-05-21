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

public class AppInfoUtils {

    /**
     * List of field names might contain string which will be recognized by Scanner a numeric type. 
     * (Scanner is locale specific as well)
     */
    protected List<String> stringFields;

    public AppInfoUtils(String...fields) {
        stringFields = new ArrayList<String>();

        for (String f: fields) stringFields.add(f);
    }

    /**
     * 
     * @param node
     * @param json
     */
    protected void setAppInfoDynamicFormsJsonValue(AnyNode node, JSONObject json) {
        String name  = node.getLocalName();

        if (name.equals("additional")) {
            //simply convert the xml to json
            json.put("additional", XML.toJSONObject(node.toString(), true).getJSONObject("additional"));
        }
        else {
            String value = node.getStringValue().trim();

            if (stringFields.contains(name)) {
                json.put(name, value);
            }
            else {
                Scanner scanner = new Scanner(value);

                if      (scanner.hasNextBoolean())    json.put(name, scanner.nextBoolean());
                else if (scanner.hasNextBigDecimal()) json.put(name, scanner.nextBigDecimal());
                else if (scanner.hasNextBigInteger()) json.put(name, scanner.nextBigInteger());
                else                                  json.put(name, value);

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
    public void readAppInfoDynamicForms(Annotated aModel, JSONObject json) {
        AnyNode appInfoNode = getAppInfoNode(aModel, "dynamicForms");

        if (appInfoNode != null) {
            AnyNode child = appInfoNode.getFirstChild(); //stupid API, there is no getChildren

            if (child != null) {
                if (child.getNodeType() == AnyNode.ELEMENT) setAppInfoDynamicFormsJsonValue(child, json);

                for (child = child.getNextSibling(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == AnyNode.ELEMENT) setAppInfoDynamicFormsJsonValue(child, json);
                }
            }
        }
    }
}
