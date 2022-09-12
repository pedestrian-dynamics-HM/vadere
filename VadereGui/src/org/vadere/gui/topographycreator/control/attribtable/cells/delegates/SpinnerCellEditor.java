package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;

import javax.swing.*;
import javax.swing.text.NumberFormatter;

public class SpinnerCellEditor extends AttributeEditor {
    private JSpinner spinner;

    public SpinnerCellEditor(AttributeTree.TreeNode model, JPanel contentPanel) {
        super(model, contentPanel);
    }

    @Override
    protected void initialize() {
        this.spinner = new JSpinner();
        JFormattedTextField txt = ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        ((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);
        this.add(spinner);
        this.spinner.addChangeListener(e -> updateModel(spinner.getValue()));
    }

    public void onModelChanged(Object value) {
        this.spinner.setValue(value);
        spinner.revalidate();
        spinner.repaint();
    }
}
