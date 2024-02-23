/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*

Schema('CrudEntity_Dependecy', 0) {
    struct(name: 'CrudEntity_Dependecy', useSequence: true) {
        field(name: 'MemberPath', type: 'string', multiplicity: '0..1')
        field(name: 'MemberName', type: 'string', multiplicity: '0..1')
        field(name: 'MemberSlotId', type: 'integer', multiplicity: '0..1')
        field(name: 'MemberProperties', type: 'string', multiplicity: '0..1')
    }
}

Script('CrudEntity_ChangeDependecy', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    script('groovy', moduleDir+'/script/CrudEntity_ChangeDependecy.groovy')
}

Activity('CrudEntity_ChangeDependecy', 0) {
    Property((PREDEFINED_STEP): 'TO BE OVERRIDDEN')
    Property((DEPENDENCY_NAME): 'TO BE OVERRIDDEN')

    Schema($crudEntity_Dependecy_Schema)
    Script($crudEntity_ChangeDependecy_Script)
}
