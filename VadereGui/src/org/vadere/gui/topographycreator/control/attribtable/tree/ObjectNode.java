package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.lang.reflect.Field;

public class ObjectNode extends AttributeTreeModel.TreeNode {

    public ObjectNode(AttributeTreeModel.TreeNode parent, Field field) {
        super(parent, field);
        setValueNode(new ValueNode(parent, field == null ? "" : field.getName(), field == null ? null : field.getType(), null));
    }

    public ObjectNode(AttributeTreeModel.TreeNode parent, String fieldName, Class fieldType) {
        super(parent, fieldName,fieldType);
        setValueNode(new ValueNode(parent, fieldName, fieldType, null));
    }


    @Override
    public void updateParentsFieldValue(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        var field = getChildren().get(fieldName).getFirst();
        field.setAccessible(true);
        field.set(getReference(), object);
        field.setAccessible(false);
        getParent().updateParentsFieldValue(getFieldName(), getReference());
    }

}
