package org.vadere.gui.topographycreator.control.attribtable.cells.editors;


import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.EventObject;
import java.util.HashMap;

public class FieldValueEditor extends JPanel implements TableCellEditor {

    private HashMap<String, JComponent> editorObjects;

    public FieldValueEditor(){
        super();
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
        return false;
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
