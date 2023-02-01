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
PropertyDescriptionList('ActivityDesc', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'ActivityDesc')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('Agent', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'Agent')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('CompositeActivityDesc', 0) {
    PropertyDesc(name: 'Name',       isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',       isMutable: false, isClassIdentifier: true,  defaultValue: 'ActivityDesc')
    PropertyDesc(name: 'Complexity', isMutable: false, isClassIdentifier: true,  defaultValue: 'Composite')
    PropertyDesc(name: 'Module',     isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('ItemDescription', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'ItemDescription')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('ElementaryActivityDesc', 0) {
    PropertyDesc(name: 'Name',       isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',       isMutable: false, isClassIdentifier: true,  defaultValue: 'ActivityDesc')
    PropertyDesc(name: 'Complexity', isMutable: false, isClassIdentifier: true,  defaultValue: 'Elementary')
    PropertyDesc(name: 'Module',     isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('Module', 0) {
    PropertyDesc(name: 'Name',      isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',      isMutable: false, isClassIdentifier: true,  defaultValue: 'Module')
    PropertyDesc(name: 'Version',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Namespace', isMutable: true , isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('Schema', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'Schema')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('Query', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'Query')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('Script', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'Script')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('StateMachine', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'StateMachine')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('PropertyDescription', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'PropertyDescription')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('AgentDesc', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'AgentDesc')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('ItemDesc', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'ItemDesc')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('RoleDesc', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'RoleDesc')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}

PropertyDescriptionList('DomainContext', 0) {
    PropertyDesc(name: 'Name',   isMutable: true , isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'DomainContext')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: '')
}
