package org.vadere.gui.components.control.simulation;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.state.health.InfectionStatus;

import javax.swing.*;
import java.awt.*;

public class ActionSetInfectionStatusColor extends ActionSetColor {
    private final JComboBox<InfectionStatus> comboBox;

    public ActionSetInfectionStatusColor(final String name, final SimulationModel<? extends DefaultSimulationConfig> model, final JPanel coloredPanel,
                                      final JComboBox<InfectionStatus> comboBox) {
        super(name, model, coloredPanel);
        this.comboBox = comboBox;
    }

    @Override
    protected void saveColor(Color color) {
        InfectionStatus infectionStatus = comboBox.getItemAt(comboBox.getSelectedIndex());

        if (infectionStatus != null) {
            model.config.setInfectionStatusColor(infectionStatus, color);
        }
    }
}