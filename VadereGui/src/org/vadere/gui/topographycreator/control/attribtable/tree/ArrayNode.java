package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.apache.commons.math3.util.Pair;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArrayNode extends AttributeTree.TreeNode {

    private final Class internalType;

    private final Field field;
    private Constructor internalDefaultConstructor;
    private List dummy;

    public ArrayNode(AttributeTree.TreeNode parent, Field field) {
        super(parent, field.getName(), field.getType());
        this.field = field;
        internalType = (Class) (((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);

        try {
            if (internalType.equals(Integer.class) || internalType.equals(Double.class)) {
            } else {
                internalDefaultConstructor = internalType.getDeclaredConstructor(null);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find a default constructor for class " + internalType + "when creating ArrayNode");
        }
    }


    public void remove(String field) throws IllegalAccessException {
        var array = (ArrayList) getReference();
        var idx = Integer.parseInt(field);
        array.remove(Integer.parseInt(field));
        shiftMapKeys(field, idx);
        try {
            getParent().updateParentsFieldValue(getFieldName(), getReference());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        notifyStructureListeners();
        notifyValueListeners();
    }

    private void shiftMapKeys(String field, int idx) {
        ((HashMap) getChildren()).remove(field);
        while (getChildren().containsKey(String.valueOf(++idx))) {
            getChildren().put(String.valueOf(idx - 1), getChildren().get(String.valueOf(idx)));
        }
        ((HashMap) getChildren()).remove(String.valueOf(idx - 1));
    }


    @Override
    public void updateStructure(Object Object) {

    }

    @Override
    public void updateValues(Object obj) throws IllegalAccessException {
        if (obj != null) {
            if (obj.equals(getReference()))
                return;
            setReference(obj);
            var array = (ArrayList) obj;
            var children = ((HashMap) getChildren());
            for (int i = 0; i < array.size(); i++) {
                var key = String.valueOf(i);
                var value = array.get(i);
                if (children.containsKey(key)) {
                    var node = ((FieldNode) ((Pair) children.get(key)).getSecond());
                    try {
                        node.getValueNode().setValue(array.get(i));
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    var newNode = new FieldNode(this, key, value.getClass(), new ValueNode(this, key, value.getClass(), value));
                    children.put(key, new Pair(null, newNode));//new ValueNode(this, key, value.getClass(), value)
                }
            }
        }
        notifyStructureListeners();
        notifyValueListeners();
    }

    @Override
    public void updateParentsFieldValue(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        var array = (ArrayList) getReference();
        var idx = Integer.parseInt(fieldName);
        array.set(idx, object);
        getParent().updateParentsFieldValue(getFieldName(), array);
    }

    @Override
    public Field getField() {
        return this.field;
    }

    public void addElement() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        String key = String.valueOf(getChildren().size());
        Object newInstance = null;
        if (Modifier.isAbstract(internalType.getModifiers())) {
            newInstance = null;
        } else if (internalType.equals(Integer.class)) {
            newInstance = Integer.valueOf(0);
        } else if (internalType.equals(Double.class)) {
            newInstance = new Double(0.0);
        } else {
            newInstance = internalDefaultConstructor.newInstance(null);
        }
        var newNode = new FieldNode(this, key, internalType, new ValueNode(this, key, internalType, newInstance));
        getChildren().put(key, new Pair(null, newNode));
        ((ArrayList) getReference()).add(newInstance);

        try {
            getParent().updateParentsFieldValue(getFieldName(), getReference());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        notifyStructureListeners();
        notifyValueListeners();
    }
}
