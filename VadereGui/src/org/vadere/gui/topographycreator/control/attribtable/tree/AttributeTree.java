package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.vadere.gui.topographycreator.control.attribtable.cells.editors.EditorRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import static org.vadere.gui.topographycreator.control.attribtable.util.ClassFields.getSuperDeclaredFields;

public class AttributeTree {
    private static final EditorRegistry registry = EditorRegistry.getInstance();
    private final TreeNode rootNode;

    public AttributeTree(String fieldName, Class baseClass) {
        rootNode = parseClassTree(null, fieldName, baseClass);
    }

    public static TreeNode parseClassTree(TreeNode parent, String fieldName, Class baseClass) {
        Field[] fields = getSuperDeclaredFields(baseClass);//baseClass.getDeclaredFields();

        TreeNode root = new ObjectNode(parent, fieldName, baseClass);

        {
            for (var field : fields) {
                Class type = field.getType();
                String name = field.getName();
                if (registry.contains(type)) {
                    root.children.put(name, new FieldNode(root, name, type));
                }
                else if (isArray(type)) {
                    root.children.put(name, new ArrayNode(root, name, type));
                } else if (Modifier.isAbstract(type.getModifiers())) {
                    root.children.put(name, new OptionalNode(root, name, type));
                } else {
                    root.children.put(name, parseClassTree(root, name, type));
                }
            }
        }

        return root;
    }

    private static boolean isArray(Class clazz) {
        return clazz.isAssignableFrom(ArrayList.class);
        //return clazz.getComponentType() != null;
    }

    public TreeNode getRootNode() {
        return rootNode;
    }

    public void updateModel(Object obj) throws AttributTreeException, IllegalAccessException {
        if (obj.getClass() != rootNode.getFieldClass())
            throw new AttributTreeException();
        rootNode.updateModel(obj);

    }

    public static abstract class TreeNode {
        private TreeNode parent;
        private final HashMap<String, TreeNode> children;

        private Object reference;
        private final Class clazz;

        private final String name;

        private final ArrayList<ModelChangeListener> listeners;

        public TreeNode(TreeNode parent, String fieldName, Class clazz) {
            this.children = new HashMap<>();
            this.clazz = clazz;
            this.name = fieldName;
            this.parent = parent;
            this.listeners = new ArrayList<>();
        }

        public TreeNode getParent() {
            return parent;
        }

        public void setParent(TreeNode parent) {
            this.parent = parent;
        }

        public HashMap<String, TreeNode> getChildren() {
            return children;
        }

        public TreeNode find(String path) throws AttributTreeException {
            String splitPath = path.substring(0, path.indexOf("."));
            if (!this.children.containsKey(splitPath)) {
                throw new AttributTreeException();
            }
            TreeNode child = this.children.get(splitPath);
            return child.find(path.substring(path.indexOf(".") + 1));
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

        public String getFieldName() {
            return name;
        }


        public abstract void set(String field, TreeNode object);

        public abstract Object get(String field);

        protected void updateModel(Object obj) throws IllegalAccessException, AttributTreeException {
            //if(getReference() == null || obj.getClass() != getReference().getClass())
            //     parent.set(getFieldName(),parseClassTree(obj.getClass()));
            if (obj != null) {
                setReference(obj);
                var fields = getSuperDeclaredFields(obj.getClass());
                for (var field : fields) {
                    var name = field.getName();
                    var node = children.get(name);
                    field.setAccessible(true);
                    node.updateModel(field.get(obj));
                    field.setAccessible(false);
                }
                notifyListeners(obj);
            }
        }

        public void addChangeListener(ModelChangeListener listener) {
            this.listeners.add(listener);
        }

        protected void notifyListeners(Object obj) {
            for (var listener : listeners) {
                listener.modelChanged(obj);
            }
        }

        protected abstract void revalidateObjectStructure(String field,Object object) throws NoSuchFieldException, IllegalAccessException;
    }
}

