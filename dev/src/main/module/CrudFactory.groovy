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
Schema('CrudFactory_NewInstanceDetails', 0) {
    struct(name: 'CrudFactory_NewInstanceDetails', useSequence: true) {
        field(name: 'Name',      type: 'string',  documentation: 'The Name of the new instance, it can be generated')
        field(name: 'SubFolder',  type: 'string',  documentation: 'Put the new Item into this sub-forlder', multiplicity: '0..1')
        struct(name: 'SchemaInitialise', useSequence: true, multiplicity: '0..1') {
            dynamicForms (hidden: true)
            anyField()
        }
    }
}

Script("CrudFactory_InstantiateItem", 0) {
    script('groovy', moduleDir+'/script/CrudFactory_InstantiateItem.groovy')
}

Script("CrudEntity_ChangeName", 0) {
    script('groovy', moduleDir+'/script/CrudEntity_ChangeName.groovy')
}

Activity('CrudFactory_InstantiateItem', 0) {
    Property('PredefinedStep': 'CreateItemFromDescription')
    Property('OutcomeInit': 'Empty')
    
    Schema($crudFactory_NewInstanceDetails_Schema)
    Script($crudFactory_InstantiateItem_Script)
}

Workflow('CrudFactory_Workflow', 0) {
    ElemActDef($crudFactory_InstantiateItem_ActivityDef)
}
