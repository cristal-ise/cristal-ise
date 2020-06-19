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
Activity('CreateNewLocalObjectDef', 0) {
    Property(Description: 'Create a new C2KLocalObject Definition')
    AbstractProperty(NewType: '')

    Schema($newDevObjectDef_Schema)
    Script($localObjectDefCreator_Script)
}

Activity('CreateAgent', 0) {
    Property(Description: 'Create a new Agent from its Description')

    Schema($newAgent_Schema)
    Script($instantiateAgent_Script)
}

Activity('CreateItem', 0) {
    Property(Description: 'Create a new Item from its Description')

    Schema($newDevObjectDef_Schema)
    Script($instantiateItem_Script)
}

Activity('DefineNewCollection', 0) {
    Property(Description: '')

    Schema($newCollection_Schema)
    Script($collDescCreator_Script)
}

Activity('EditPropertyDescription', 0) {
    Property(Description: 'Set the initial properties for new instances.')

    Schema('PropertyDescription', 0)
}

Activity('SetInstanceWorkflow', 0) {
    Property(Description: 'Choose a CompositeActivityDefinition to use for the workflow of new instances')

    Schema($chooseWorkflow_Schema)
    Script($setWorkflow_Script)
}
