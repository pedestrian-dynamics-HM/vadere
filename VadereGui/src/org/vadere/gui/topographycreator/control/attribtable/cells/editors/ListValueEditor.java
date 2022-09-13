package org.vadere.gui.topographycreator.control.attribtable.cells.editors;


import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

public class ListValueEditor extends JPanel implements TableCellEditor {

    private final AttributeEditor editor;
    private final String id;

    public ListValueEditor(AttributeEditor editor, String id) {
        this.editor = editor;
        this.id = id;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        var panel = new JPanel(new BorderLayout());
        var lable = new JLabel(id);
        lable.setBorder(new EmptyBorder(0, 8, 0, 8));
        panel.add(lable, BorderLayout.WEST);
        panel.add(editor);
        return panel;
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
        return false;
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
