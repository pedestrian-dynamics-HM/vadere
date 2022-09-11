package org.vadere.gui.topographycreator.control.attribtable.tree;

public class MissingNode extends AttributeTree.TreeNode {

    public MissingNode(String clazzName, Class clazz) {
        super(clazzName, clazz);
    }

    @Override
    public void set(String field, AttributeTree.TreeNode object) {

    }


    @Override
    public Object get(String field) {
        return null;
    }

    @Override
    protected void revalidateObjectStructure(String field, Object object) throws NoSuchFieldException, IllegalAccessException {

    }
}
