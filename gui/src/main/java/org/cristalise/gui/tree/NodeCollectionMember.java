package org.cristalise.gui.tree;

import org.cristalise.gui.ItemTabManager;
import org.cristalise.kernel.collection.CollectionMember;

import lombok.Getter;

@Getter
public class NodeCollectionMember extends NodeItem {
    protected CollectionMember member;

    public NodeCollectionMember(CollectionMember m, ItemTabManager desktop) {
        super(m.getItemPath(), desktop);
        member = m;
    }
}
