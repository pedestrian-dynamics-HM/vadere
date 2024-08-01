package org.vadere.gui.components.control.simulation;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.state.psychology.information.InformationState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ActionSetImageOverlay extends ActionSetColor {
    private final JTextField textField;
    private final JPanel panel;

    public ActionSetImageOverlay(final String name, final SimulationModel<? extends DefaultSimulationConfig> model, JPanel panel,
                                 final JTextField textField) {
        super(name, model, panel);
        this.textField = textField;
        this.panel = panel;
    }

    @Override
    public void actionPerformed(final ActionEvent event) {

        super.actionPerformed(event);
    }

    @Override
    protected void saveColor(Color color) {

        double i = 5.0;


    }
}
