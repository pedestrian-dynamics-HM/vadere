package org.vadere.gui.topographycreator.control.attribtable.tree;

public class ValueNode extends AttributeTree.TreeNode {


    public ValueNode(AttributeTree.TreeNode parent, String fieldName, Class clazz, Object value) {
        super(parent, fieldName, clazz);
        setReference(value);
    }

    @Override
    public void set(String field, AttributeTree.TreeNode object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String field) {
        throw new UnsupportedOperationException();
    }

    public Object getValue() {
        return getReference();
    }

    public void setValue(Object value) throws NoSuchFieldException, IllegalAccessException {
        if (!value.equals(getReference())) {
            setReference(value);
            setParentField(getFieldName(), getReference());
        }
    }

    @Override
    public void updateModel(Object obj) throws IllegalAccessException, TreeException {
        setReference(obj);
        notifyListeners(obj);
    }

    @Override
    public void setParentField(String field, Object object) throws NoSuchFieldException, IllegalAccessException {
        getParent().setParentField(getFieldName(), object);
    }
}
