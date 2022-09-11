package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;

import javax.swing.*;

public class EmptyEditor extends AttributeEditor {
    public EmptyEditor(AbstractModel parent, String id, JPanel contentPanel) {
        super(parent, id, contentPanel);
    }

    @Override
    protected void initialize() {

    }

    @Override
    protected void modelChanged(Object value) {

    }
}
