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

public class ItemPredefinedStepContainer extends PredefinedStepContainer {

    @Override
    public void createChildren() {
        predInit("AddDomainPath", "Adds a new path to this item in the LDAP domain tree", new AddDomainPath());
        predInit("RemoveDomainPath", "Removes an existing path to this item from the LDAP domain tree", new RemoveDomainPath());
        predInit("ReplaceDomainWorkflow", "Replaces the domain CA with the supplied one. Used by the GUI to save new Wf layout", new ReplaceDomainWorkflow());
        predInit("AddC2KObject", "Adds or overwrites a C2Kernel object for this Item", new AddC2KObject());
        predInit("RemoveC2KObject", "Removes the named C2Kernel object from this Item.", new RemoveC2KObject());
        predInit("WriteProperty", "Writes a property to the Item", new WriteProperty());
        predInit("WriteViewpoint", "Writes a viewpoint to the Item", new WriteViewpoint());
        predInit(RemoveViewpoint.class.getSimpleName(), RemoveViewpoint.description, new RemoveViewpoint());
        predInit("AddNewCollectionDescription", "Creates a new collection description in this Item", new AddNewCollectionDescription());
        predInit("CreateNewCollectionVersion", "Creates a new numbered collection version in this Item from the current one.", new CreateNewCollectionVersion());
        predInit("AddNewSlot", "Creates a new slot in the given aggregation, that holds instances of the item description of the given key", new AddNewSlot());
        predInit("AssignItemToSlot", "Assigns the referenced item to a pre-existing slot in an aggregation", new AssignItemToSlot());
        predInit("ClearSlot", "Clears an aggregation member slot, given a slot no or item uuid", new ClearSlot());
        predInit("RemoveSlotFromCollection", RemoveSlotFromCollection.description, new RemoveSlotFromCollection());
        predInit("AddMemberToCollection",    AddMemberToCollection.description,    new AddMemberToCollection());
        predInit(AddMembersToCollection.class,      AddMembersToCollection.description,      new AddMembersToCollection());
        predInit(RemoveMembersFromCollection.class, RemoveMembersFromCollection.description, new RemoveMembersFromCollection());
        predInit(UpdateDependencyMember.class,      UpdateDependencyMember.description,      new UpdateDependencyMember());
        predInit("Import", "Imports an outcome into the Item, with a given schema and viewpoint", new Import());
        predInit("CreateAgentFromDescription", "Create a new agent using this item as its description", new CreateAgentFromDescription());
        predInit(ChangeName.class, ChangeName.description, new ChangeName());
        predInit(Erase.class,      Erase.description,      new Erase());
        predInit(BulkErase.class,  BulkErase.description,  new BulkErase());

        predInit(UpdateCollectionsFromDescription.class, UpdateCollectionsFromDescription.description, new UpdateCollectionsFromDescription());
        predInit(UpdateProperitesFromDescription.class,  UpdateProperitesFromDescription.description,  new UpdateProperitesFromDescription());

        predInit(ImportImportAgent.class, ImportImportAgent.description, new ImportImportAgent());
        predInit(ImportImportItem.class,  ImportImportItem.description,  new ImportImportItem());
        predInit(ImportImportRole.class,  ImportImportRole.description,  new ImportImportRole());

        //UpdateImportReport class is not added to the container because it can only be used during bootstrap

        predInit("CreateItemFromDescription", "Create a new item using this item as its description", new CreateItemFromDescription());
    }
}
