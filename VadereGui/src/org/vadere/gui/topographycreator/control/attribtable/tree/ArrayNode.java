package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.util.ArrayList;
import java.util.HashMap;

public class ArrayNode extends AttributeTree.TreeNode {


    public ArrayNode(AttributeTree.TreeNode parent, String fieldName, Class clazz) {
        super(parent, fieldName, clazz);
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
        var array = (ArrayList) obj;
        var children = ((HashMap) getChildren());
        for (int i = 0; i < array.size(); i++) {
            var key = String.valueOf(i);
            var value = array.get(i);
            if (children.containsKey(key)) {
                var node = (ValueNode) children.get(key);
                node.setValue(array.get(i));
            } else {
                children.put(key, new ValueNode(this, key, value.getClass(), value));
            }
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
