package org.vadere.gui.topographycreator.control.attribtable.cells.renderer;

import org.vadere.util.reflection.VadereAttribute;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.lang.reflect.Field;

public class FieldNameRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
    private static final Color Transparent = new Color(0, 0, 0, 0);
    private Font font = UIManager.getFont("Label.font");
    private final String id;

    public FieldNameRenderer(String id) {
        this.id = id;
        this.font = new Font(font.getName(), Font.BOLD, font.getSize());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Field field = (Field) value;
        JPanel fieldNamePanel = new JPanel(new BorderLayout());

        JLabel nameTextPane = new JLabel();
        JLabel typeTextPane = new JLabel();

        nameTextPane.setText(id);
        //typeTextPane.setText(getType(field));

        nameTextPane.setBackground(Transparent);
        typeTextPane.setBackground(Transparent);

        nameTextPane.setIcon(null);

        typeTextPane.setFont(font.deriveFont(Font.BOLD, 10));
        typeTextPane.setForeground(new Color(0, 192, 163));

        nameTextPane.setHorizontalAlignment(JLabel.LEFT);
        typeTextPane.setHorizontalAlignment(JLabel.RIGHT);
        fieldNamePanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        fieldNamePanel.add(nameTextPane,BorderLayout.PAGE_START);
        //fieldNamePanel.add(typeTextPane,BorderLayout.PAGE_END);

        return fieldNamePanel;
    }

    private String getType(Field field) {
        return field.getType().getSimpleName();
    }

    private String getName(Field field){
        var annots = field.getDeclaredAnnotations();
        String annotationName = field.getAnnotation(VadereAttribute.class).name();
        String fieldName = field.getName();

        return annotationName.equals("") ? fieldName : annotationName;
    }
}
