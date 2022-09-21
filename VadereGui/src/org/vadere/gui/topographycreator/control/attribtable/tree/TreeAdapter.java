package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;

public class TreeAdapter extends AttributeTreeModel.TreeNode {
    private final Revalidatable rev;

    public TreeAdapter(Revalidatable rev) {
        super(null, null);
        this.rev = rev;
    }
    @Override
    public void updateValues(Object obj){

    }
    @Override
    public void updateParentsFieldValue(String fieldName, Object object) {
        rev.revalidateObjectStructure(object);
    }

}
