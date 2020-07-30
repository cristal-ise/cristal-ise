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
Schema('ChooseWorkflow', 0) {
    struct(name: 'ChooseWorkflow', useSequence: true) {
        field(name: 'WorkflowDefinitionName', type: 'string', documentation: 'Give the name of the composite activity description that you would like new instance of this description to run')
        field(name: 'WorkflowDefinitionVersion', type: 'string', documentation: 'Give the version of this activity that you would like to use.')
    }
}

Schema('New', 0, new File('src/main/resources/boot/OD/New_0.xsd'))

Schema('NewAgent', 0) {
    struct(name: 'NewAgent', useSequence: true) {
        field(name: 'Name', type: 'string', documentation: 'Please give a name for your new Agent.')
        field(name: 'SubFolder', type: 'string', documentation: 'If you want to store your object in a subfolder, give the subpath here.')
        field(name: 'InitialRoles', type: 'string', documentation: 'Comma separated list of Roles.')
        field(name: 'Password', type: 'string', multiplicity: '0..1', documentation: 'Initial password (optional).')
    }
}

Schema('NewCollection', 0) {
    struct(name: 'NewCollection', useSequence: true) {
        field(name: 'Name', type: 'string')
        field(name: 'Type', type: 'string', values: ['Dependency', 'Aggregation'])
    }
}

Schema('NewDevObjectDef', 0) {
    struct(name: 'NewDevObjectDef', useSequence: true) {
        field(name: 'ObjectName', type: 'string', documentation: 'Please give a name for your new object.')
        field(name: 'SubFolder',  type: 'string', documentation: 'If you want to store your object in a subfolder, give the subpath here.')
    }
}
