/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.gui.tabs.collection;

import javax.swing.JPanel;

import org.cristalise.gui.MainFrame;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.process.Gateway;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CollectionView<M extends CollectionMember> extends JPanel {

    protected Collection<M> thisColl;
    protected ItemProxy item;

    public CollectionView() {
        super();
    }

    public void setItem(ItemProxy item) {
        this.item = item;
    }

    public abstract void setCollection(Collection<M> coll);

    public Collection<? extends CollectionMember> getCollection() {
        return thisColl;
    }

    protected void saveCollection() {
        try {
            String[] params = new String[1];
            params[0] = Gateway.getMarshaller().marshall(thisColl);
            MainFrame.userAgent.execute(item, "AddC2KObject", params);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
