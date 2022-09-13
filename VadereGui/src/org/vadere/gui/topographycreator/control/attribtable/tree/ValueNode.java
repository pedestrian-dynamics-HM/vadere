package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.lang.reflect.Field;

public class ValueNode extends AttributeTree.TreeNode {


    public ValueNode(AttributeTree.TreeNode parent, String fieldName, Class clazz, Object value) {
        super(parent, fieldName, clazz);
        setReference(value);
    }

    @Override
    public void updateStructure(Object Object) {
        // Do nothing
    }

    public Object getValue() {
        return getReference();
    }

    public void setValue(Object value) throws NoSuchFieldException, IllegalAccessException {
        if (value == null) {
            setReference(null);
            updateParentsFieldValue(getFieldName(), getReference());
            return;
        }

        if (!value.equals(getReference())) {
            setReference(value);
            updateParentsFieldValue(getFieldName(), getReference());
        }
    }

    @Override
    public void updateValues(Object obj) throws IllegalAccessException, TreeException {
        setReference(obj);
        notifyValueListeners();
    }

    @Override
    public void updateParentsFieldValue(String field, Object object) throws NoSuchFieldException, IllegalAccessException {
        getParent().updateParentsFieldValue(getFieldName(), object);
    }

    @Override
    public Field getField() {
        return null;
    }
}
