package org.vadere.gui.components.control.simulation;

import java.awt.Color;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.vadere.gui.components.control.simulation.ActionSetColor;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

public class ActionSetPedestrianColor extends ActionSetColor {
    private final JComboBox<Integer> comboBox;

    public ActionSetPedestrianColor(final String name, final SimulationModel<? extends DefaultSimulationConfig> model, final JPanel coloredPanel,
                                    final JComboBox<Integer> comboBox) {
        super(name, model, coloredPanel);
        this.comboBox = comboBox;
    }

    @Override
    protected void saveColor(Color color) {
        Integer selectedTargetId = comboBox.getItemAt(comboBox.getSelectedIndex());

        if (selectedTargetId != null) {
            model.config.setPedestrianColor(selectedTargetId, color);
        }
    }
}
