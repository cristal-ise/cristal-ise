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
import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.cristalise.kernel.common.*;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.IOException;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
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
        this.setBuiltInProperty(SCHEMA_NAME, "Collection");
    }

    //Creates a new member slot for the given item in a dependency, and assigns the item
    public static final String description = "Adds members to a given item";
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException, ObjectNotFoundException,
            InvalidCollectionModification {
                try {
                    Dependency input = (Dependency) Gateway.getMarshaller().unmarshall(requestData);
                    String collectionName = input.getName();
                    Dependency dep = (Dependency) Gateway.getStorage().get(item, COLLECTION+"/"+collectionName+"/last", locker);
                    for (DependencyMember member: input.getMembers().list) {
                        if (dep.containsBuiltInProperty(MEMBER_ADD_SCRIPT)) {
                            CastorHashMap scriptProps = new CastorHashMap();
                            scriptProps.put("collection", dep);
                            scriptProps.put("member", member);
            
                            evaluateScript(item, (String)dep.getBuiltInProperty(MEMBER_ADD_SCRIPT), scriptProps, locker);
                        }
                        DependencyMember newMember = dep.createMember(member.getItemPath(), member.getProperties());
                        dep.addMember(newMember);
                    }
    
                    Gateway.getStorage().put(item, dep, locker);
    
                    return Gateway.getMarshaller().marshall(dep);
                } catch (IOException | ValidationException | MarshalException | MappingException ex) {
                    log.error("Error adding members to collection", ex);
                    throw new InvalidDataException("Error adding members to collection: " + ex);
                }
                
    }
}
