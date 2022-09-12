package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.vadere.gui.topographycreator.control.attribtable.ModelListener;
import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.EditorRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static org.vadere.gui.topographycreator.control.attribtable.util.ClassFields.getSuperDeclaredFields;

public class AttributeTree {
    private static final EditorRegistry registry = EditorRegistry.getInstance();
    private final TreeNode rootNode;
    private final Class rootClass;

    private final Revalidatable revalidatable;

    public AttributeTree(String fieldName, Class baseClass, Revalidatable revalidatable) {
        rootNode = parseClassTree(new RootNodeWrapper(revalidatable), fieldName, baseClass);
        this.rootClass = baseClass;
        this.revalidatable = revalidatable;
    }

    public static TreeNode parseClassTree(TreeNode parent, String fieldName, Class baseClass) {
        Field[] fields = getSuperDeclaredFields(baseClass);

        TreeNode root = new ObjectNode(parent, fieldName, baseClass);

        for (var field : fields) {
            Class type = field.getType();
            String name = field.getName();
            switch (TYPE_OF(type)) {
                case REGISTERED:
                    root.children.put(name, new FieldNode(root, field));
                    break;
                case ARRAY:
                    root.children.put(name, new ArrayNode(root, field));
                    break;
                case ABSTRACT:
                    root.children.put(name, new ObjectNode(root, fieldName, type));
                    break;
                default:
                    root.children.put(name, parseClassTree(root, name, type));
            }
        }

        return root;
    }

    private static TYPE TYPE_OF(Class clazz) {
        if (registry.contains(clazz)) {
            return TYPE.REGISTERED;
        }
        if (isArray(clazz)) {
            return TYPE.ARRAY;
        }
        if (Modifier.isAbstract(clazz.getModifiers())) {
            return TYPE.ABSTRACT;
        }
        return TYPE.OBJECT;
    }

    private static boolean isArray(Class clazz) {
        return clazz.isAssignableFrom(List.class);
    }

    public Class getRootClass() {
        return rootClass;
    }

    public TreeNode getRootNode() {
        return rootNode;
    }

    public void updateModel(Object obj) throws TreeException, IllegalAccessException {
        if (obj.getClass() != rootNode.getFieldClass())
            throw new TreeException();
        rootNode.updateModel(obj);

    }

    private enum TYPE {
        REGISTERED, ARRAY, ABSTRACT, OBJECT
    }

    public static abstract class TreeNode {
        private TreeNode parent;
        private final LinkedHashMap<String, TreeNode> children;

        private Object reference;
        private final Class clazz;

        private final String name;


        private final ArrayList<ModelListener> listeners;

        public TreeNode(TreeNode parent, String fieldName, Class clazz) {
            this.children = new LinkedHashMap<>();
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

        public TreeNode find(String path) throws TreeException {
            String splitPath = path.substring(0, path.indexOf("."));
            if (!this.children.containsKey(splitPath)) {
                throw new TreeException();
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

        public void updateModel(Object obj) throws IllegalAccessException, TreeException {
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

        public void addChangeListener(ModelListener listener) {
            this.listeners.add(listener);
        }

        protected void notifyListeners(Object obj) {
            for (var listener : listeners) {
                listener.modelChanged(obj);
            }
        }

        public abstract void setParentField(String field, Object object) throws NoSuchFieldException, IllegalAccessException;
    }
}

