package org.vadere.gui.projectview.utils;

import java.awt.*;

import javax.swing.*;

public class ClassRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Class)
            this.setText(((Class) value).getSimpleName());

        return c;
    }
}
