package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;
import org.vadere.gui.topographycreator.control.attribtable.tree.FieldNode;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collections;

public class SpinnerCellEditor extends AttributeEditor {
    private JSpinner spinner;

    @Override
    public java.util.List<Component> getInputComponent() {
        return Collections.singletonList(
                ((JSpinner.NumberEditor) spinner.getEditor()).getTextField());
    }

    public SpinnerCellEditor(AttributeTreeModel.TreeNode model, JPanel contentPanel, Object initialValue) {
        super(model, contentPanel,initialValue);
    }

    @Override
    protected void initialize(Object initialValue) {
        this.spinner = new JSpinner();
        initializeSpinnerModel();
        if(initialValue!=null)
            this.spinner.setValue(initialValue);
        initializeSpinnerValue();
        initializeSpinnerListener();
        this.add(spinner);
    }

    private void initializeSpinnerModel() {
        JFormattedTextField txt = ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        // allow invalid true to allow a single '-' minus Symbol to be valid
        ((NumberFormatter) txt.getFormatter()).setAllowsInvalid(true);
    }

    private void initializeSpinnerValue() {
        var value = ((FieldNode) model).getValueNode().getValue();
        if (value != null)
            this.spinner.setValue(((FieldNode) model).getValueNode().getValue());
    }

    private void initializeSpinnerListener() {
        this.spinner.addChangeListener(e -> updateModel(spinner.getValue()));
    }

    public void onModelChanged(Object value) {
        this.spinner.setValue(value);
    }
}
