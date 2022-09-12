package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.lang.reflect.Field;

public class FieldNode extends AttributeTree.TreeNode {
    private final ValueNode valueNode;
    private final Field field;

    public FieldNode(AttributeTree.TreeNode parent, Field field) {
        super(parent, field.getName(), field.getType());
        this.field = field;
        this.valueNode = new ValueNode(parent, field.getName(), field.getType(), null);
    }

    @Override
    public void set(String field, AttributeTree.TreeNode object) {
        var value = ((ValueNode) object).getValue();
        setReference(value);
    }

    @Override
    public Object get(String field) {
        return getReference();
    }

    @Override
    public void updateModel(Object obj) throws IllegalAccessException, TreeException {
        valueNode.updateModel(obj);
        notifyListeners(obj);
    }

    @Override
    public void setParentField(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        getFieldClass().getDeclaredField(fieldName).set(getReference(), object);
        getParent().setParentField(getFieldName(), getReference());
    }

    public ValueNode getValueNode() {
        return this.valueNode;
    }
}
