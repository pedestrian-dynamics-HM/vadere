package org.vadere.gui.topographycreator.control.attribtable.tree;

public class OptionalNode extends AttributeTree.TreeNode {
    private AttributeTree.TreeNode node;

    public OptionalNode(AttributeTree.TreeNode parent, String fieldName, Class clazz) {
        super(parent, fieldName, clazz);
    }

    @Override
    public void set(String field, AttributeTree.TreeNode object) {
        node.set(field, object);
    }

    @Override
    protected void updateModel(Object obj) throws IllegalAccessException, AttributTreeException {
        node = AttributeTree.parseClassTree(getParent(), getFieldName(), obj.getClass());
        node.updateModel(obj);
    }

    @Override
    public Object get(String field) {
        return node.get(field);
    }

    @Override
    protected void revalidateObjectStructure(String field, Object object) throws NoSuchFieldException, IllegalAccessException {

    }
}
