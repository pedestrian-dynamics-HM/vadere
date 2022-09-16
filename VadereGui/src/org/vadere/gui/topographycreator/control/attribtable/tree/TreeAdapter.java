package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;

public class TreeAdapter extends AttributeTree.TreeNode {
    private final Revalidatable rev;

    public TreeAdapter(Revalidatable rev) {
        super(null, null);
        this.rev = rev;
    }

    @Override
    public void updateStructure(Object Object) {

    }

    @Override
    public void updateValues(Object obj) throws IllegalAccessException, TreeException {

    }

    @Override
    public void updateParentsFieldValue(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        //getFieldClass().getDeclaredField(fieldName).set(getReference(),object);
        rev.revalidateObjectStructure(object);
    }

}
