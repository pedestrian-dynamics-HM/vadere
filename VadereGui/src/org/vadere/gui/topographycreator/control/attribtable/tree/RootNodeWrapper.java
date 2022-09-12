package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;

public class RootNodeWrapper extends AttributeTree.TreeNode {
    private final Revalidatable rev;

    public RootNodeWrapper(Revalidatable rev) {
        super(null, null, null);
        this.rev = rev;
    }

    @Override
    public void set(String field, AttributeTree.TreeNode object) {
        //throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setParentField(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        //getFieldClass().getDeclaredField(fieldName).set(getReference(),object);
        rev.revalidateObjectStructure(object);
    }
}
