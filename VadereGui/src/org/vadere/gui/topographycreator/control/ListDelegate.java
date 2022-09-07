package org.vadere.gui.topographycreator.control;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class ListDelegate extends JPanel {
    JTable table;

    JComponent editor;

    public ListDelegate(JTable table) {
        super(new BorderLayout());
        this.table = table;
        this.editor = new JPanel();
    }

    public ListDelegate(JTable table, Class<? extends JComponent> editorClass) {
        super(new BorderLayout());
        this.table = table;
        try {
            this.editor = editorClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDelegate(int row) {
        this.removeAll();
        if (row == table.getModel().getRowCount() - 1) {
            var panel = new JPanel(new BorderLayout());
            this.add(panel, BorderLayout.CENTER);
            return;
        }
        var panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(String.valueOf(row)), BorderLayout.WEST);
        panel.add(editor);
        this.add(panel, BorderLayout.CENTER);
    }
}
