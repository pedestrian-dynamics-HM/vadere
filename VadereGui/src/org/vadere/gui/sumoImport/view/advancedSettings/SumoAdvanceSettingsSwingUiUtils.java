package org.vadere.gui.sumoImport.view.advancedSettings;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.utils.Resources;
import org.vadere.util.config.VadereConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SumoAdvanceSettingsSwingUiUtils {
    private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale")*0.7);

    public static double ParseDouble(JTextField textField){
        return Double.parseDouble(textField.getText());
    }

    public static int ParseInt(JTextField textField){
        return Integer.parseInt(textField.getText());
    }

    public static Double ParseNullableDouble(JTextField textField){
        if(textField.getText().isEmpty()) return null;

        try{
            return Double.parseDouble(textField.getText());
        }catch(NumberFormatException e){
            return null;
        }
    }

    public static JPanel addBorder(JPanel content, String title){
        content.setBorder(new TitledBorder(title));
        return content;
    }

    public static JPanel createLabeledComponent(String title, Component content) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        panel.add(new JLabel(title));
        panel.add(Box.createHorizontalStrut(5));
        panel.add(content);

        return panel;
    }

    public static JPanel createLabeledComponentWithTooltip(String title, Component content, String tooltip) {
        JPanel panel = createLabeledComponent(title, content);
        panel.add(Box.createHorizontalStrut(5));
        addTooltipButton(panel, tooltip);
        return panel;
    }

    public static JPanel createLabeledComponent(String title, Component content, String unit) {
        JPanel panel = createLabeledComponent(title, content);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(new JLabel(unit));

        return panel;
    }

    public static JPanel createLabeledComponentWithTooltip(String title, Component content, String unit, String tooltip) {
        JPanel panel = createLabeledComponent(title, content, unit);
        panel.add(Box.createHorizontalStrut(5));
        addTooltipButton(panel, tooltip);

        return panel;
    }

    public static JPanel createLabeledDisableableComponent(String title, Component content, JCheckBox checkBoxToDisableContent) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JLabel label = new JLabel(title);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                checkBoxToDisableContent.setSelected(!checkBoxToDisableContent.isSelected());
            }
        });
        panel.add(label);
        panel.add(checkBoxToDisableContent);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(content);

        checkBoxToDisableContent.addChangeListener(changeEvent -> {
            content.setEnabled(checkBoxToDisableContent.isSelected());
        });
        content.setEnabled(checkBoxToDisableContent.isSelected());

        return panel;
    }

    private static void addTooltipButton(JPanel toAddTo, String tooltip) {
        ImageIcon originalIcon = new ImageIcon(Resources.class.getResource("/icons/info_icon.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        JButton infoButton = new JButton(scaledIcon);
        infoButton.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        infoButton.setBorderPainted(false);
        infoButton.setContentAreaFilled(false);
        infoButton.setFocusPainted(false);
        infoButton.setOpaque(false);
        setClickableTooltip(tooltip, infoButton);

        toAddTo.add(infoButton);
    }

    private static void setClickableTooltip(String tooltip, JButton infoButton) {
        infoButton.setToolTipText(tooltip);
        infoButton.addActionListener(e -> {
            // force open tooltip on click
            int oldDelay = ToolTipManager.sharedInstance().getInitialDelay();
            ToolTipManager.sharedInstance().setInitialDelay(0);
            ToolTipManager.sharedInstance().mouseMoved(
                    new MouseEvent(infoButton, 0, 0, 0, 0, 0, 0, false)
            );
            ToolTipManager.sharedInstance().setInitialDelay(oldDelay);
        });
    }

    public static JPanel createLabeledDisableableComponent(String title, Component content, JCheckBox checkBoxToDisableContent, String unit) {
        JPanel panel = createLabeledDisableableComponent(title, content, checkBoxToDisableContent);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(new JLabel(unit));

        return panel;
    }

    public static JPanel createLabeledDisableableComponent(String title, Component content, JCheckBox checkBoxToDisableContent, String unit, String tooltip) {
        JPanel panel = createLabeledDisableableComponent(title, content, checkBoxToDisableContent, unit);
        panel.add(Box.createHorizontalStrut(5));
        addTooltipButton(panel, tooltip);

        return panel;
    }

    public static JPanel createLabeledDisableableComponentWithTooltip(String title, Component content, JCheckBox checkBoxToDisableContent,  String tooltip) {
        JPanel panel = createLabeledDisableableComponent(title, content, checkBoxToDisableContent);
        panel.add(Box.createHorizontalStrut(5));
        addTooltipButton(panel, tooltip);

        return panel;
    }

    public static JTextField createDoubleTextField(){
        JTextField textField = new JTextField(15);

        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string.matches("^-?\\d*(\\.\\d*)?$")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches("^-?\\d*(\\.\\d*)?$")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        return textField;
    }

    public static JTextField createIntegerTextField(){
        JTextField textField = new JTextField(15);

        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string.matches("^-?\\d*")) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text.matches("^-?\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        return textField;
    }

    @NotNull
    public static GridBagConstraints createSettingsGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        return gbc;
    }
}
