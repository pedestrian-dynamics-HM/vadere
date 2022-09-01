package org.vadere.gui.topographycreator.control.cellrenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.HashMap;

public class FieldValueRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
    HashMap<String, JComponent> editorObjects;

    public FieldValueRenderer(){
    }

    public void setEditors(HashMap<String, JComponent> editorObjects) {
        this.editorObjects = editorObjects;
    }
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return editorObjects.get(((Field) table.getValueAt(row, 0)).getName());
    }
}
