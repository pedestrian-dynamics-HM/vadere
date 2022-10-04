package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;
import org.vadere.gui.topographycreator.control.attribtable.tree.FieldNode;

import javax.swing.*;
import java.awt.*;

public abstract class AttributeEditor extends JPanel implements AttributeTreeModel.ViewListener, AttributeTreeModel.ValueListener {

    protected final AttributeTreeModel.TreeNode model;
    protected JPanel contentPanel;

    Object oldValue;
    private boolean locked = false;

    public abstract java.util.List<Component> getInputComponent();

    public AttributeEditor(AttributeTreeModel.TreeNode model, JPanel contentPanel, Object initialValue) {
        super(new BorderLayout());
        this.model = model;
        this.model.addChangeListener(this);
        this.contentPanel = contentPanel;
        disableNotify();
        initialize(initialValue);
        enableNotify();
    }

    protected abstract void initialize(Object initialValue);

    protected abstract void onModelChanged(Object object);

    public void modelChanged(Object fieldValue) {
        if (oldValue != fieldValue) {
            oldValue = fieldValue;
            disableNotify();
            onModelChanged(fieldValue);
            enableNotify();
            revalidate();
            repaint();
        }
    }

    private void disableNotify() {
        this.locked = true;
    }

    private void enableNotify(){
        this.locked = false;
    }

    protected boolean canUpdate() {
        return !locked;
    }

    public void updateModel(Object value) {
        if (canUpdate()) updateModelFromValue(value);
    }

    protected void updateModelFromValue(Object newValue) {
        try {
            ((FieldNode) this.model).getValueNode().setValue(newValue);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}