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
        predInit(AddC2KObject.class);
        predInit(AddDomainPath.class);
        predInit(AddMembersToCollection.class);
        predInit(AddNewCollectionDescription.class);
        predInit(AddNewSlot.class);
        predInit(AssignItemToSlot.class);
        predInit(BulkErase.class);
        predInit(ChangeName.class);
        predInit(ClearSlot.class);
        predInit(CreateAgentFromDescription.class);
        predInit(CreateItemFromDescription.class);
        predInit(CreateNewCollectionVersion.class);
        predInit(Erase.class);
        predInit(Import.class);
        predInit(ImportImportAgent.class);
        predInit(ImportImportItem.class);
        predInit(ImportImportRole.class);
        predInit(RemoveC2KObject.class);
        predInit(RemoveDomainPath.class);
        predInit(RemoveMembersFromCollection.class);
        predInit(RemoveViewpoint.class);
        predInit(ReplaceDomainWorkflow.class);
        predInit(UpdateCollectionsFromDescription.class);
        predInit(UpdateDependencyMember.class);
        predInit(WriteProperty.class);
        predInit(WriteViewpoint.class);
        //UpdateImportReport class is not added to the container because it can only be used during bootstrap
        predInit(UpdateProperitesFromDescription.class);
    }
}
