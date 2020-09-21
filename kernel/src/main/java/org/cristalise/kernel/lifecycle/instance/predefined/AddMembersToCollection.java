/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.lifecycle.instance.predefined;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.MEMBER_ADD_SCRIPT;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.cristalise.kernel.common.*;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;

@Slf4j
public class AddMembersToCollection extends AddMemberToCollection {

    public AddMembersToCollection(){
        super();
        this.setBuiltInProperty(SCHEMA_NAME, "BulkAddMembers");
    }

    //Creates a new member slot for the given item in a dependency, and assigns the item
    public static final String description = "Adds members to a given item";
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException, ObjectNotFoundException,
            InvalidCollectionModification{
                NodeList members = getMembers(requestData);
                Dependency dep = getDependency();
                String[] params = null;
                for (int i=0; i < members.getLength(); i++) {
                    params = unpackParamsAndGetCollection(item, members.item(i).getNodeValue(), locker);
                    DependencyMember member = null;
                    log.debug("parammmmmmmmmmmmmmmmmmmm");
                    log.debug(String.valueOf(dep));
                    log.debug(String.valueOf(MEMBER_ADD_SCRIPT));
                    log.debug(MEMBER_ADD_SCRIPT.getName());
                    log.debug(String.valueOf(dep.containsBuiltInProperty(MEMBER_ADD_SCRIPT)));
        
                    // find member and assign entity
                    if (memberNewProps == null) member = dep.createMember(childPath);
                    else                        member = dep.createMember(childPath, memberNewProps);
        
                    if (dep.containsBuiltInProperty(MEMBER_ADD_SCRIPT)) {
                        CastorHashMap scriptProps = new CastorHashMap();
                        scriptProps.put("collection", dep);
                        scriptProps.put("member", member);
        
                        evaluateScript(item, (String)dep.getBuiltInProperty(MEMBER_ADD_SCRIPT), scriptProps, locker);
                    }
        
                    dep.addMember(member);
        
                    Gateway.getStorage().put(item, dep, locker);
        
                    //put ID of the newly created member into the return data of this step
                    params = Arrays.copyOf(params, params.length+1);
                    params[params.length-1] = Integer.toString(member.getID());
                }
                if (params != null) {
                    return bundleData(params);
                } else {
                    return null;
                }
    }


    public static String bundleData(String[] data) {
        //TODO: Implement bundleData for BulkAddMembers schema
        return null;
    }

    public static NodeList getMembers(String xmlData) {
        try {
            Document scriptDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xmlData)));

            NodeList nodeList = scriptDoc.getElementsByTagName("Member");
            return nodeList;
        }
        catch (Exception ex) {
            log.error("", ex);
        }
        return null;
    }
}
