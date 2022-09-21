package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AbstrNode extends AttributeTree.TreeNode {

    private final Map<Class<?>,AttributeTree.TreeNode> classRegistry;
    private ObjectNode node;

    private ValueNode valueNode;
    public AbstrNode(AttributeTree.TreeNode parent, Field field) {
        super(parent, field.getName(), field.getType());
        node  = new ObjectNode(getParent(), field);
        valueNode = new ValueNode(this,field.getName(), field.getType(),null);
        this.classRegistry = new Reflections("org.vadere")
                .getSubTypesOf(field.getType())
                .stream()
                .collect(Collectors.toMap(
                        aClass -> aClass,
                        aClass -> AttributeTree.parseClassTree(getParent(),field.getName(),aClass)));
    }

    @Override
    public void updateValues(Object obj){
        if (obj == null) {
            this.node = null;
            setReference(null);
        } else {
            this.node = (ObjectNode) classRegistry.get(obj.getClass());
            try {
                this.node.updateValues(obj);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (TreeException e) {
                throw new RuntimeException(e);
            }
            setReference(obj);
        }
        notifyValueListeners();
    }

    @Override
    public void updateParentsFieldValue(String field, Object object) throws NoSuchFieldException, IllegalAccessException {
        var nextNode = classRegistry.get(object.getClass());
        if(nextNode == null){
            throw  new RuntimeException("the object "+ object+" which was tried to be assigned to AbstrNode is not a subclass of "+getFieldType());
        }
        node = (ObjectNode) nextNode;
        try {
            node.updateValues(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (TreeException e) {
            throw new RuntimeException(e);
        }
        getParent().updateParentsFieldValue(field,object);
    }

    public Map<Class<?>,AttributeTree.TreeNode>getSubClassModels(){
        return this.classRegistry;
    }

    public ValueNode getValueNode(){
        return valueNode;
    }

}
