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
package org.cristalise.kernel.collection;

/**
 * Helper enumeration to make built-in Collection names easier to maintain and document
 */
public enum BuiltInCollections {
    /**
     * CollectionDescription of CompositeActivityDef. It is instantiated as .... Also it is Dependency Collection of Workflow.
     */
    ACTIVITY("Activity"),
    ACTIVITY_PRIME("Activity'"),

    /**
     * 
     */
    AGGREGATE_SCRIPT("AggregateScript"),
    AGGREGATE_SCRIPT_PRIME("AggregateScript'"),

    /**
     * Dependency Collection of Module. ....
     */
    CONTENTS("Contents"),
    CONTENTS_PRIME("Contents'"),

    /**
     * Dependency Collection of Script. ...
     * 
     */
    INCLUDE("Include"),
    INCLUDE_PRIME("Include'"),

    /**
     * 
     */
    MASTER_SCHEMA("MasterSchema"),
    MASTER_SCHEMA_PRIME("MasterSchema'"),

    /**
     * CollectionDescription of elementary ActivityDef. It is instantiated as two Activity properties (see bellow).
     * Also it is Dependency Collection of Activity.
     * 
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#SCHEMA_NAME
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#SCHEMA_VERSION
     */
    SCHEMA("Schema"),
    SCHEMA_PRIME("Schema'"),

    /**
     * 
     */
    SCHEMA_INITIALISE("SchemaInitialise"),
    SCHEMA_INITIALISE_PRIME("SchemaInitialise'"),

    /**
     * CollectionDescription of elementary ActivityDef. It is instantiated as two Activity properties (see bellow). 
     * Also it is Dependency Collection of Workflow.
     * 
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#SCRIPT_NAME
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#SCRIPT_VERSION
     */
    SCRIPT("Script"),
    SCRIPT_PRIME("Script'"),

    /**
     * CollectionDescription of elementary ActivityDef. It is instantiated as two Activity properties (see bellow). 
     * Also it is Dependency Collection of Workflow.
     * 
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#QUERY_NAME
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#QUERY_VERSION
     */
    QUERY("Query"),
    QUERY_PRIME("Query'"),

    /**
     * CollectionDescription of elementary ActivityDef. It is instantiated as two Activity properties
     * 
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#STATE_MACHINE_NAME
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#STATE_MACHINE_VERSION
     */
    STATE_MACHINE("StateMachine"),
    STATE_MACHINE_PRIME("StateMachine'"),
    

    /**
     * Dependency collection
     */
    WORKFLOW("workflow"),
    WORKFLOW_PRIME("workflow'");


    private String collectionName;

    private BuiltInCollections(final String n) {
        collectionName = n;
    }

    public String getName() {
        return collectionName;
    }

    public String toString() {
        return getName();
    }

    public static BuiltInCollections getValue(String collName) {
        for (BuiltInCollections coll : BuiltInCollections.values()) {
            if(coll.getName().equals(collName) || coll.name().equals(collName)) return coll;
        }
        return null;
    }
}
