package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.util.HashMap;

public class ArrayNode extends AttributeTree.TreeNode {

    public ArrayNode(String clazzName, Class clazz) {
        super(clazzName, clazz);
    }

    @Override
    public void set(String field, AttributeTree.TreeNode object) {
        ((HashMap) getChildren()).put(field, object);
    }

    public void remove(String field) {
        ((HashMap) getChildren()).remove(field);
    }

    @Override
    public Object get(String field) {
        return ((HashMap) getReference()).get(field);
    }

    @Override
    protected void updateModel(Object obj) throws IllegalAccessException {
        setReference(obj);
        var array = (Object[]) obj;
        ((HashMap) getReference()).clear();
        for (int i = 0; i < array.length; i++) {
            ((HashMap) getReference()).put(String.valueOf(i), array[i]);
        }
        notifyListeners(obj);
    }

    @Override
    protected void revalidateObjectStructure(String unused1, Object unused2) throws NoSuchFieldException, IllegalAccessException {
        var len = ((HashMap) getReference()).size();
        var array = new Object[len];
        for (int i = 0; i < len; i++) {
            array[i] = ((HashMap) getReference()).get(String.valueOf(i));
        }
        getParent().revalidateObjectStructure(getFieldName(), array);
    }
}
