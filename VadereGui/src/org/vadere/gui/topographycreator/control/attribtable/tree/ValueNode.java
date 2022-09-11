package org.vadere.gui.topographycreator.control.attribtable.tree;

public class ValueNode extends AttributeTree.TreeNode {
    public ValueNode(String clazzName, Class clazz) {
        super(clazzName, clazz);
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

    public void setValue(Object value) {
        setReference(value);
    }

    @Override
    protected void revalidateObjectStructure(String field, Object object) throws NoSuchFieldException, IllegalAccessException {
        getParent().revalidateObjectStructure(getFieldName(), object);
    }
}
