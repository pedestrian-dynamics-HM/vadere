package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.lang.reflect.Field;

public class AbstrNode extends AttributeTree.TreeNode {
    public AbstrNode(AttributeTree.TreeNode parent, String fieldName, Class clazz) {
        super(parent, fieldName, clazz);
    }

    @Override
    public void updateParentsFieldValue(String field, Object object) throws NoSuchFieldException, IllegalAccessException {

    }

    @Override
    public Field getField() {
        return null;
    }
}
