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
states = ['ACTIVE', 'INACTIVE']

Activity('CrudState_Activate', 0) {
    Property('ItemProperty.State': states[0])
}

Activity('CrudState_Deactivate', 0) {
    Property('ItemProperty.State': states[1])
}

Workflow('CrudState_Manage', 0) {
    Layout {
        LoopInfinitive {
            OrSplit(RoutingExpr: 'property//State') {
                Block(Alias: 'INACTIVE')  { Act('Activate',   $crudState_Activate_ActivityDef) }
                Block(Alias: '!INACTIVE') { Act('Deactivate', $crudState_Deactivate_ActivityDef) }
            }
        }
    }
}
