package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.apache.commons.math3.util.Pair;
import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;
import org.vadere.gui.topographycreator.control.attribtable.ValueListener;
import org.vadere.gui.topographycreator.control.attribtable.cells.EditorRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.vadere.gui.topographycreator.control.attribtable.util.ClassFields.getSuperDeclaredFields;

public class AttributeTree {
    private static final EditorRegistry registry = EditorRegistry.getInstance();
    private final TreeNode rootNode;
    private final Class rootClass;

    private final Revalidatable revalidatable;

    public AttributeTree(String fieldName, Class baseClass, Revalidatable revalidatable) {
        rootNode = parseClassTree(new TreeAdapter(revalidatable), fieldName, baseClass);
        this.rootClass = baseClass;
        this.revalidatable = revalidatable;
    }

    public static TreeNode parseClassTree(TreeNode parent, String fieldName, Object object) {
        Field[] fields = getSuperDeclaredFields(object.getClass());

        TreeNode root = new ObjectNode(parent, null);

        for (var field : fields) {
            var type = field.getType();

            String name = field.getName();
            switch (TYPE_OF(type)) {
                case REGISTERED:
                    root.children.put(name, new Pair<>(field, new FieldNode(root, field)));
                    break;
                case ARRAY:
                    root.children.put(name, new Pair<>(field, new ArrayNode(root, field)));
                    break;
                case ABSTRACT:
                    root.children.put(name, new Pair<>(field, new AbstrNode(root, field)));
                    break;
                case ABSTRACT_CHILD: {
                    root.children.put(name, new Pair<>(field, new AbstrNode(root, field, true)));
                    break;
                }
                default:
                    root.children.put(name, new Pair<>(field, parseClassTree(root, name, type.getGenericSuperclass().getClass())));
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
        var superClass = clazz.getSuperclass();
        if (superClass != null) {
            if (Modifier.isAbstract(superClass.getModifiers())) {
                return TYPE.ABSTRACT_CHILD;
            }
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
        if (obj.getClass() != rootNode.getFieldType())
            throw new TreeException();
        rootNode.updateValues(obj);

    }

    private enum TYPE {
        REGISTERED, ARRAY, ABSTRACT, ABSTRACT_CHILD, OBJECT
    }

    public static abstract class TreeNode {
        private final String fieldName;
        private final Class fieldType;
        private TreeNode parent;
        private final LinkedHashMap<String, Pair<Field, TreeNode>> children;
        /**
         * the type which a node represents
         */
        private final Field field;
        private final ArrayList<ValueListener> valueListeners;
        private final ArrayList<StructureListener> structureListeners;
        /**
         * The instance of a type which the node represents
         */
        private Object reference;

        public TreeNode(TreeNode parent) {
            this(parent, null);
        }

        public TreeNode(TreeNode parent, Field field) {
            this.children = new LinkedHashMap<>();
            this.field = field;
            this.fieldType = field == null ? null : field.getType();
            this.fieldName = field == null ? "" : field.getName();
            this.parent = parent;
            this.valueListeners = new ArrayList<>();
            this.structureListeners = new ArrayList<>();
        }

        public TreeNode(TreeNode parent, String fieldName, Class fieldType) {
            this.children = new LinkedHashMap<>();
            this.field = null;
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.parent = parent;
            this.valueListeners = new ArrayList<>();
            this.structureListeners = new ArrayList<>();
        }

        public TreeNode getParent() {
            return parent;
        }

        public void setParent(TreeNode parent) {
            this.parent = parent;
        }

        public Map<String, Pair<Field, TreeNode>> getChildren() {
            return children;
        }

        public TreeNode find(String path) throws TreeException {
            String splitPath = path.substring(0, path.indexOf("."));
            if (!this.children.containsKey(splitPath)) {
                throw new TreeException();
            }
            TreeNode child = this.children.get(splitPath).getSecond();
            return child.find(path.substring(path.indexOf(".") + 1));
        }

        public Object getReference() {
            return reference;
        }

        protected void setReference(Object reference) {
            this.reference = reference;
        }

        public Class getFieldType() {
            return fieldType;
        }

        public String getFieldName() {
            return fieldName;
        }


        //public abstract void set(String field, TreeNode object);

        //public abstract TreeNode get(String field);

        /**
         * Sends a structure update through the attribute tree. These updates are necessary for
         * abstract types to update their children. After a structure update the editors need to be recreated.
         *
         * @param
         */
        public void updateStructure(Object object) throws IllegalAccessException {
        }

        private boolean isReferenceNotSet() {
            return getReference() == null;
        }

        /**
         * Sends a value update through the tree. This update sets the fields.
         *
         * @param obj
         * @throws IllegalAccessException
         * @throws TreeException          node.updateValue(obj){... valueNode.setValue(obj); notifyChangeListeners()...}
         */
        public void updateValues(Object obj) throws IllegalAccessException, TreeException {
            setReference(obj);
            for (var child : children.values()) {
                var field = child.getFirst();
                var node = child.getSecond();
                field.setAccessible(true);
                if (obj == null)
                    node.updateValues(null);
                else
                    node.updateValues(field.get(obj));
                field.setAccessible(false);
            }
            notifyValueListeners();
        }

        public void addChangeListener(ValueListener listener) {
            this.valueListeners.add(listener);
        }

        public void addStructureListener(StructureListener listener) {
            this.structureListeners.add(listener);
        }

        protected void notifyValueListeners() {
            for (var listener : valueListeners) {
                listener.modelChanged(getReference());
            }
        }

        protected void notifyStructureListeners() {
            for (var listener : structureListeners) {
                listener.structureChanged(this);
            }
        }

        public TreeNode get(String field) {
            return children.get(field).getSecond();
        }

        public Field getField() {
            return this.field;
        }

        public abstract void updateParentsFieldValue(String field, Object object) throws NoSuchFieldException, IllegalAccessException;

    }
}

