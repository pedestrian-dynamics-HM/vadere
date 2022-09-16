package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.lang.reflect.Field;

public class ObjectNode extends AttributeTree.TreeNode {
    private final ValueNode node;

    public ObjectNode(AttributeTree.TreeNode parent, Field field) {
        super(parent, field);
        this.node = new ValueNode(parent, field == null ? "" : field.getName(), field == null ? null : field.getType(), null);
    }


    @Override
    public void updateParentsFieldValue(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        var field = getChildren().get(fieldName).getFirst();
        field.setAccessible(true);
        field.set(getReference(), object);
        field.setAccessible(false);
        getParent().updateParentsFieldValue(getFieldName(), getReference());
    }

    public ValueNode getValueNode() {
        return this.node;
    }

}
