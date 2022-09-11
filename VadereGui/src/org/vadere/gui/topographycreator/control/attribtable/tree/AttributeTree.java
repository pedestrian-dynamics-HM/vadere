package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.vadere.gui.topographycreator.control.attribtable.cells.editors.EditorRegistry;
import org.vadere.util.reflection.VadereAttribute;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static org.vadere.gui.topographycreator.control.attribtable.util.ClassFields.getSuperDeclaredFields;

public class AttributeTree {
    private static EditorRegistry registry = EditorRegistry.getInstance();
    private TreeNode rootNode;

    public AttributeTree(Class baseClass){
        rootNode = parseClassTree(baseClass);
    }

    public static TreeNode parseClassTree(Class baseClass){
        Field[] fields = getSuperDeclaredFields(baseClass);

        TreeNode root = new ObjectNode(null,baseClass);

        {
            for(var field : fields){
                if(field.getAnnotation(VadereAttribute.class)==null)
                    continue;;
                Class type = field.getType();
                String name = field.getName();
                if(registry.contains(type)){
                   root.children.put(name,new FieldNode(name,type));
                }
                else if(isArray(type)){
                    root.children.put(name,new ArrayNode(name,type));
                }
                else {
                    root.children.put(name,parseClassTree(type));
                }
            }
        }

        return root;
    }

    private static boolean isArray(Class clazz){
        return clazz.getComponentType() != null;
    }

    public void updateModel(Object obj) throws AttributTreeException, IllegalAccessException {
        if(obj.getClass() != rootNode.getFieldClass())
            throw new AttributTreeException();
        rootNode.updateModel(obj);

    }


    public static abstract class TreeNode {
        private TreeNode parent;
        private HashMap<String, TreeNode> children;

        private Object reference;
        private Class clazz;

        private String name;

        private ArrayList<ModelChangeListener> listeners;

        public TreeNode(String clazzName,Class clazz){
            this.children = new HashMap<>();
            this.clazz = clazz;
            this.name = clazzName;

            this.listeners = new ArrayList<>();
        }

        public TreeNode getParent() {
            return parent;
        }

        public void setParent(TreeNode parent) {
            this.parent = parent;
        }

        public Collection<TreeNode> getChildren() {
            return children.values();
        }

        public TreeNode find(String path) throws AttributTreeException {
            String splitPath = path.substring(0,path.indexOf("."));
            if(!this.children.containsKey(splitPath)){
                throw new AttributTreeException();
            }
            TreeNode child = this.children.get(splitPath);
            return child.find(path.substring(path.indexOf(".")+1,path.length()));
        }

        public Object getReference() {
            return reference;
        }

        public void setReference(Object reference) {
            this.reference = reference;
        }

        public Class getFieldClass() {
            return clazz;
        }

        public String getFieldName(){
            return name;
        }


        public abstract void set(String field, TreeNode object);
        public abstract Object get(String field);

        protected void updateModel(Object obj) throws IllegalAccessException {
            if(getReference() == null || obj.getClass() != getReference().getClass())
                parent.set(getFieldName(),parseClassTree(obj.getClass()));
            setReference(obj);
            var fields =getSuperDeclaredFields(obj.getClass());
            for(var field : fields){
                var name = field.getName();
                var node = children.get(name);
                field.setAccessible(true);
                node.updateModel(field.get(obj));
                field.setAccessible(false);
            }
            notifyListeners(obj);
        }

        public void addChangeListener(ModelChangeListener listener){
            this.listeners.add(listener);
        }

        protected void notifyListeners(Object obj){
            for(var listener : listeners){
                listener.modelChanged(obj);
            }
        }

        protected abstract void revalidateObjectStructure(String field,Object object) throws NoSuchFieldException, IllegalAccessException;
    }
}

