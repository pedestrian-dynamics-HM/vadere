package org.vadere.gui.topographycreator.control.attribtable.cells.editors;


import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;
import java.util.HashMap;

public class FieldValueEditor extends JPanel implements TableCellEditor {

    private HashMap<String, JComponent> editorObjects;
    private AttributeEditor editor;

    public FieldValueEditor(AttributeEditor editor) {
        super();
    }

    public void set(HashMap<String, JComponent> editorObjects) {
        this.editorObjects = editorObjects;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return editor;
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
