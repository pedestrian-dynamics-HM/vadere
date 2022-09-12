package org.vadere.gui.topographycreator.control.attribtable.tree;

public class ObjectNode extends AttributeTree.TreeNode {


    public ObjectNode(AttributeTree.TreeNode parent, String fieldName, Class clazz) {
        super(parent, fieldName, clazz);
    }

    @Override
    public void set(String field, AttributeTree.TreeNode object) {
        object.setParent(this);
        getChildren().put(field, object);
    }

    @Override
    public Object get(String field) {
        return getChildren().get(field);
    }

    @Override
    protected void revalidateObjectStructure(String field, Object object) throws NoSuchFieldException, IllegalAccessException {
        var fieldObj = getReference().getClass().getField(field);
        fieldObj.set(getReference(), object);
        getParent().revalidateObjectStructure(getFieldName(), getReference());
    }
}
