package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.lang.reflect.Field;

public class ObjectNode extends AttributeTree.TreeNode {

    public ObjectNode(AttributeTree.TreeNode parent, String fieldName, Class clazz) {
        super(parent, fieldName, clazz);
    }


    @Override
    public void updateParentsFieldValue(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        var field = getChildren().get(fieldName).getFirst();
        field.setAccessible(true);
        field.set(getReference(), object);
        field.setAccessible(false);
        getParent().updateParentsFieldValue(getFieldName(), getReference());
    }

    @Override
    public Field getField() {
        return null;
    }
}
