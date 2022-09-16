package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.apache.commons.math3.util.Pair;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ArrayNode extends AttributeTree.TreeNode {

    private final Class internalType;

    private Constructor internalDefaultConstructor;
    private List dummy;

    public ArrayNode(AttributeTree.TreeNode parent, Field field) {
        super(parent, field);
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
        var children = super.getChildren();
        children.remove(field);
        while (children.containsKey(String.valueOf(++idx))) {
            children.put(String.valueOf(idx - 1), children.get(String.valueOf(idx)));
        }
        children.remove(String.valueOf(idx - 1));
    }


    @Override
    public void updateStructure(Object Object) {
        notifyStructureListeners();
        notifyValueListeners();
    }

    @Override
    public void updateValues(Object obj) throws IllegalAccessException {
        if (obj != null) {
            if (obj.equals(getReference()))
                return;
            setReference(obj);
            var array = (ArrayList)obj;
            var children = super.getChildren();
            if(children.size() > array.size()){
                for(int i = array.size(); i <= children.size(); i++){
                    children.remove(String.valueOf(i));
                }
            }
            for (int i = 0; i < array.size(); i++) {
                var key = String.valueOf(i);
                var value = array.get(i);
                if (children.containsKey(key)) {
                    var node = ((FieldNode) ((Pair) children.get(key)).getSecond());
                    node.setValueNode(new ValueNode(this, key, internalType, value));
                } else {
                    var newNode = new FieldNode(this, key, internalType, new ValueNode(this, key, internalType, value));
                    children.put(key, new Pair(null, newNode));//new ValueNode(this, key, value.getClass(), value)
                }
            }
            if(children.size() > array.size()){
                for(int i = array.size(); i < children.size(); i++){
                    children.remove(String.valueOf(i));
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
        ((FieldNode)super.getChildren().get(fieldName).getSecond()).setValueNode(new ValueNode(getParent(),fieldName,internalType,object));
        array.set(idx, object);
        getParent().updateParentsFieldValue(getFieldName(), array);
    }

    public void addElement() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        String key = String.valueOf(super.getChildren().size());
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
        super.getChildren().put(key, new Pair(null, newNode));
        ((ArrayList) getReference()).add(newInstance);

        try {
            getParent().updateParentsFieldValue(getFieldName(), getReference());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        notifyStructureListeners();
        notifyValueListeners();
    }

    @Override
    public Map<String, Pair<Field, AttributeTree.TreeNode>> getChildren() {
        return new TreeMap(super.getChildren());
    }
}
