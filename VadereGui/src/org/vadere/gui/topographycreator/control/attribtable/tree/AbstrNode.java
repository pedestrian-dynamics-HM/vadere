package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.lang.reflect.Field;

public class AbstrNode extends AttributeTree.TreeNode {

    private ObjectNode node;

    public AbstrNode(AttributeTree.TreeNode parent, Field field) {
        super(parent, field.getName(), field.getType());
        new ObjectNode(getParent(), field);
    }

    public AbstrNode(AttributeTree.TreeNode parent, Field field, boolean child) {
        super(parent, field.getName(), field.getType().getSuperclass());
        new ObjectNode(getParent(), field);
    }


    @Override
    public void updateValues(Object obj) throws IllegalAccessException, TreeException {
        if (obj == null) {
            node.setReference(null);
        } else {
            if (getReference() == null) {
                node = (ObjectNode) AttributeTree.parseClassTree(getParent(), getFieldName(), obj.getClass());
            } else {
                if (getReference().getClass() != obj.getClass()) {
                    node = (ObjectNode) AttributeTree.parseClassTree(getParent(), getFieldName(), obj.getClass());
                }
            }
        }
        setReference(obj);
        notifyStructureListeners();
        notifyValueListeners();
    }

    @Override
    public void updateParentsFieldValue(String field, Object object) throws NoSuchFieldException, IllegalAccessException {
        throw new UnsupportedOperationException();
    }

}
