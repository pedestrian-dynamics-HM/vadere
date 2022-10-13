package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.apache.commons.math3.util.Pair;

import javax.swing.*;
import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ArrayNode extends AttributeTreeModel.TreeNode {

    private final Class genericType;
    private Constructor internalDefaultConstructor;

    public ArrayNode(AttributeTreeModel.TreeNode parent, Field field) {
        super(parent, field);
        genericType = (Class) (((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);

        try {
            if (genericType.equals(Integer.class) || genericType.equals(Double.class)) {
            } else {
                internalDefaultConstructor = genericType.getDeclaredConstructor(null);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find a default constructor for class " + genericType + "when creating ArrayNode");
        }
        setValueNode(new ValueNode(getParent(),getFieldName(), getFieldType(),null));
    }


    public void remove(String field) throws IllegalAccessException {
        var array = (List) getReference();
        var idx = Integer.parseInt(field);
        array.remove(Integer.parseInt(field));
        getValueNode().setReference(getReference());
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
        var children = super.getChildren();
        children.remove(field);
        while (children.containsKey(String.valueOf(++idx))) {
            children.put(String.valueOf(idx - 1), children.get(String.valueOf(idx)));
        }
        children.remove(String.valueOf(idx - 1));
    }


    @Override
    public void updateValues(Object obj) throws IllegalAccessException {
        if (obj != null) {
            if (obj.equals(getReference()))
                return;
            setReference(obj);
            getValueNode().setReference(obj);
            var array = (List)obj;
            var children = super.getChildren();
            // 1. clear old array values
            children.clear();
            // 2. create new array values based on obj data.
            for (int i = 0; i<array.size();i++) {
                var key = String.valueOf(i);
                var value = array.get(i);
                AttributeTreeModel.TreeNode newNode = null;
                if (Modifier.isAbstract(genericType.getModifiers())) {
                    newNode = new AbstrNode(this, key, genericType);
                } else {
                    newNode = new FieldNode(this, key, genericType, new ValueNode(this, key, genericType, value));
                }

                children.put(key, new Pair(null, newNode));//new ValueNode(this, key, value.getClass(), value)
            }
        }
        notifyStructureListeners();
        notifyValueListeners();
    }

    @Override
    public void updateParentsFieldValue(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        SwingUtilities.invokeLater(()->{
        var array = (List) getReference();
        var idx = Integer.parseInt(fieldName);
        (super.getChildren().get(fieldName).getSecond()).setValueNode(new ValueNode(this,fieldName, genericType,object));
        array.set(idx, object);
            try {
                getParent().updateParentsFieldValue(getFieldName(), array);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void addElement() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        String key = String.valueOf(super.getChildren().size());
        Object newInstance = null;
        AttributeTreeModel.TreeNode newNode;
        getValueNode().setReference(getReference());
        if (Modifier.isAbstract(genericType.getModifiers())) {
            newInstance = null;
            newNode = new AbstrNode(this,key,getGenericType());
        } else {
            if (genericType.equals(Integer.class)) {
                newInstance = Integer.valueOf(0);
            } else if (genericType.equals(Double.class)) {
                newInstance = new Double(0.0);
            } else {
                newInstance = internalDefaultConstructor.newInstance(null);
            }
            newNode = new FieldNode(this, key, genericType, new ValueNode(this, key, genericType, newInstance));
        }
        super.getChildren().put(key, new Pair(null, newNode));
        ((List) getReference()).add(newInstance);

        try {
            getParent().updateParentsFieldValue(getFieldName(), getReference());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        notifyStructureListeners();
        notifyValueListeners();
    }

    public Class getGenericType() {
        return genericType;
    }

    @Override
    public Map<String, Pair<Field, AttributeTreeModel.TreeNode>> getChildren() {
        return new TreeMap(super.getChildren());
    }
}
