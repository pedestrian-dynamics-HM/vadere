package org.vadere.gui.topographycreator.control.celleditor;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.EventObject;
import java.util.HashMap;

public class FieldValueEditor implements TableCellEditor {
    private Object attached;
    private HashMap<String, JComponent> editorObjects;

    public FieldValueEditor(){
    }

    public void set(HashMap<String, JComponent> editorObjects, Object attached) {
        this.editorObjects = editorObjects;
        this.attached = attached;
    }

    JComponent comp;
    String name;
    Field field;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.field = (Field) table.getValueAt(row, 0);
        this.name = field.getName();
        this.comp = editorObjects.get(name);
        return comp;
    }
    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }

    @Override
    public void cancelCellEditing() {

    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {

    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {

    }
}
