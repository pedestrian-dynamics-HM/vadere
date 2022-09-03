package org.vadere.gui.topographycreator.control.celleditor;


import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.EventObject;
import java.util.HashMap;

public class FieldValueEditor extends DefaultCellEditor implements TableCellEditor {

    private HashMap<String, JComponent> editorObjects;

    public FieldValueEditor(){
        super(new JTextField());
    }

    public void set(HashMap<String, JComponent> editorObjects) {
        this.editorObjects = editorObjects;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return getComponent(table, row);
    }

    private JComponent getComponent(JTable table, int row) {
        var field = (Field) table.getValueAt(row, 0);
        var name = field.getName();
        return editorObjects.get(name);
    }

}
