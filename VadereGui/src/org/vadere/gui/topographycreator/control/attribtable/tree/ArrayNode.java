package org.vadere.gui.topographycreator.control.attribtable.tree;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

public class ArrayNode extends AttributeTree.TreeNode {


    public ArrayNode(AttributeTree.TreeNode parent, Field field) {
        super(parent, field.getName(), field.getType());
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
    public void updateModel(Object obj) throws IllegalAccessException {
        setReference(obj);
        if (obj != null) {
            var array = (ArrayList) obj;
            var children = ((HashMap) getChildren());
            for (int i = 0; i < array.size(); i++) {
                var key = String.valueOf(i);
                var value = array.get(i);
                if (children.containsKey(key)) {
                    var node = (ValueNode) children.get(key);
                    try {
                        node.setValue(array.get(i));
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    children.put(key, new ValueNode(this, key, value.getClass(), value));
                }
            }
        }
        notifyListeners(obj);
    }

    @Override
    public void setParentField(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        var array = (ArrayList) getReference();
        var idx = Integer.parseInt(fieldName);
        array.set(idx, object);
        getParent().setParentField(getFieldName(), array);
    }
}
