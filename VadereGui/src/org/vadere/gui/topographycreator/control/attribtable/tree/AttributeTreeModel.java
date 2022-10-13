package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.apache.commons.math3.util.Pair;
import org.vadere.gui.topographycreator.control.attribtable.cells.EditorRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.vadere.util.reflection.ClassFields.getSuperDeclaredFields;


public class AttributeTreeModel {
    private static final EditorRegistry registry = EditorRegistry.getInstance();

    /**
     * Builds a Tree model of class fields with no values assigned / listeners attached
     * @param parent
     * @param thisField
     * @param fieldName
     * @param clazz
     * @return
     */
    public static TreeNode parseClassTree(TreeNode parent,Field thisField, String fieldName, Class clazz) {
        Field[] fields = getSuperDeclaredFields(clazz);

        TreeNode root = new ObjectNode(parent, fieldName,clazz);

        for (var field : fields) {
            var type = field.getType();

            String name = field.getName();
            switch (TYPE_OF(type)) {
                case REGISTERED:
                case ENUM:
                    root.children.put(name, new Pair<>(field, new FieldNode(root, field)));
                    break;
                case ARRAY:
                    root.children.put(name, new Pair<>(field, new ArrayNode(root, field)));
                    break;
                case ABSTRACT:
                    root.children.put(name, new Pair<>(field, new AbstrNode(root, field)));
                    break;
                default:
                    root.children.put(name, new Pair<>(field, parseClassTree(root,field,name, type)));
            }
        }

        return root;
    }

    public static TreeNode parseClassTree(TreeNode parent,String fieldName, Class clazz) {
        return parseClassTree(parent,null,fieldName,clazz);
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
        if(clazz.isEnum()){
            return TYPE.ENUM;
        }
        return TYPE.OBJECT;
    }


    private static boolean isArray(Class clazz) {
        return clazz.isAssignableFrom(List.class);
    }

    private enum TYPE {
        REGISTERED, ARRAY, ABSTRACT, ENUM, OBJECT
    }

    public static abstract class TreeNode {
        private final String fieldName;
        private final Class fieldType;
        private TreeNode parent;

        private ValueNode valueNode;
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

        public ValueNode getValueNode(){
            return valueNode;
        }

        public void setValueNode(ValueNode valueNode){
            this.valueNode = valueNode;
        }
    }

    /**
     * A ValueListener is an Object (presumably an AttributeEditor) which gets notified by a TreeNode
     * if that TreeNode got notified by a parent TreeNode via the updateModel(..) method.
     */
    public interface ValueListener {
        void modelChanged(Object obj);
    }

    /**
     * A ViewListener is an Object (presumably an AttributeEditor) which can update a model
     * subclasses of AttributeEditor only need to call this methode with the current value
     * of the editor as the parameter.
     */
    public interface ViewListener {
        void updateModel(Object attributes);
    }

}

