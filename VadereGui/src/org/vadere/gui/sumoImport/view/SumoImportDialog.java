package org.vadere.gui.sumoImport.view;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.sumoImport.control.SumoImportDialogControl;
import org.vadere.gui.sumoImport.view.advancedSettings.SumoAdvancedSettingsPanel;
import org.vadere.gui.sumoImport.view.basicSettings.SumoImportSettingsPanel;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import java.awt.*;

public class SumoImportDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SumoImportDialog.class);
    private SumoImportDialogLoca loca = new SumoImportDialogLoca();

    private static final SumoImportSettingsPanel basicSettingsPanel = new SumoImportSettingsPanel();
    private static final SumoAdvancedSettingsPanel advancedImportSettingsPanel = new SumoAdvancedSettingsPanel();;
    private final SumoImportDialogControl control;

    public SumoImportDialog(Frame owner, SumoImportDialogControl control) {
        super(owner, "Import Sumo", true);
        this.control = control;

        createUIElements();
        setLocationRelativeTo(owner);
    }

    private void createUIElements() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Import Settings", basicSettingsPanel);
        tabbedPane.addTab("Advanced Settings", advancedImportSettingsPanel);
        add(tabbedPane, BorderLayout.CENTER);
        add(createSubmitButton(), BorderLayout.SOUTH);

        pack();
        basicSettingsPanel.onPacked();
    }

    @NotNull
    private JButton createSubmitButton() {
        JButton submitButton = new JButton("Start Import");
        submitButton.addActionListener(e -> {
            try {
                submitButton.setEnabled(false);
                control.submit(basicSettingsPanel.getImportSettings(), advancedImportSettingsPanel.getAdvanceImportSettings());

                dispose(); // this dialog
            } catch (Exception ex) {
                logger.error("Failed to parse sumo files", ex);
                JOptionPane.showMessageDialog(
                        null,
                        "An error occurred: " + ex.getMessage() + " (see logs for more details)",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                submitButton.setEnabled(true);
            }
        });
        return submitButton;
    }
}
