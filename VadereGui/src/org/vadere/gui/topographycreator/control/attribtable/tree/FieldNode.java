package org.vadere.gui.topographycreator.control.attribtable.tree;

public class FieldNode extends AttributeTree.TreeNode {

    public FieldNode(String clazzName, Class clazz) {
        super(clazzName, clazz);
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
    protected void updateModel(Object obj) throws IllegalAccessException {
        this.setReference(obj);
        notifyListeners(obj);
    }

    @Override
    protected void revalidateObjectStructure(String unused1, Object unused2) throws NoSuchFieldException, IllegalAccessException {
        getParent().revalidateObjectStructure(getFieldName(), getReference());
    }
}
