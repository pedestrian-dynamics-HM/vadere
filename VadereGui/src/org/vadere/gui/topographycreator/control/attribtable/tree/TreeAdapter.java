package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.vadere.gui.topographycreator.control.attribtable.ViewListener;

public class TreeAdapter extends AttributeTreeModel.TreeNode {
    private final ViewListener rev;

    public TreeAdapter(ViewListener rev) {
        super(null, null);
        this.rev = rev;
    }
    @Override
    public void updateValues(Object obj){

    }
    @Override
    public void updateParentsFieldValue(String fieldName, Object object) {
        rev.viewChanged(object);
    }

}
