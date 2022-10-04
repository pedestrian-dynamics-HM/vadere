package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.vadere.simulator.context.VadereContext;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *  AbstrNode represents an object field which can hold an instance of a subtype of the given field type.
 *  The AbstrNode does not notify structure listeners but value listeners if the object instance changes
 *  to a different class structure.
 */
public class AbstrNode extends AttributeTreeModel.TreeNode {
    private final Map<Class<?>, AttributeTreeModel.TreeNode> classRegistry;
    private ObjectNode node;

    public AbstrNode(AttributeTreeModel.TreeNode parent, Field field) {
        this(parent,field.getName(),field.getType());
    }

    public AbstrNode(AttributeTreeModel.TreeNode parent,String fieldName,Class fieldType){
        super(parent,fieldName,fieldType);
        node  = new ObjectNode(getParent(), fieldName,fieldType);
        setValueNode(new ValueNode(this,fieldName, fieldType,null));
        var cache = (TreeModelCache) VadereContext.getCtx("GUI").get(VadereContext.TREE_NODE_CTX);
        this.classRegistry = cache.getSubTypeOff(fieldType)
                .stream()
                .collect(Collectors.toMap(
                        aClass -> aClass,
                        aClass -> AttributeTreeModel.parseClassTree(getParent(),fieldName,(Class)aClass)));
    }

    @Override
    public void setValueNode(ValueNode valueNode) {
        updateValues(valueNode.getReference());
        super.setValueNode(valueNode);
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
        if(object !=null) {
            var nextNode = classRegistry.get(object.getClass());
            if (nextNode == null) {
                throw new RuntimeException("the object " + object + " which was tried to be assigned to AbstrNode is not a subclass of " + getFieldType());
            }
            node = (ObjectNode) nextNode;
            try {
                node.updateValues(object);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (TreeException e) {
                throw new RuntimeException(e);
            }
        }
        getParent().updateParentsFieldValue(field,object);
    }
    public Map<Class<?>, AttributeTreeModel.TreeNode>getSubClassModels(){
        return this.classRegistry;
    }
}
