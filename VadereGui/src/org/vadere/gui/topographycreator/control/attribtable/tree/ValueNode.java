package org.vadere.gui.topographycreator.control.attribtable.tree;

public class ValueNode extends AttributeTreeModel.TreeNode {

    public ValueNode(AttributeTreeModel.TreeNode parent, String fieldName, Class fieldType, Object value) {
        super(parent, fieldName, fieldType);
        setReference(value);
    }

    public Object getValue() {
        return getReference();
    }

    public void setValue(Object value) throws NoSuchFieldException, IllegalAccessException {
        if (value == null) {
            setReference(null);
            updateParentsFieldValue(getFieldName(), getReference());
            return;
        }

        if (!value.equals(getReference())) {
            setReference(value);
            updateParentsFieldValue(getFieldName(), getReference());
        }
    }
    @Override
    public void updateValues(Object obj)  {
        setReference(obj);
        notifyValueListeners();
    }
    @Override
    public void updateParentsFieldValue(String field, Object object) throws NoSuchFieldException, IllegalAccessException {
        getParent().updateParentsFieldValue(getFieldName(), object);
    }
}
