package org.vadere.gui.components.control.simulation;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.state.psychology.cognition.SelfCategory;

import javax.swing.*;
import java.awt.*;

public class ActionSetSelfCategoryColor extends ActionSetColor {
    private final JComboBox<SelfCategory> comboBox;

    public ActionSetSelfCategoryColor(final String name, final SimulationModel<? extends DefaultSimulationConfig> model, final JPanel coloredPanel,
                                      final JComboBox<SelfCategory> comboBox) {
        super(name, model, coloredPanel);
        this.comboBox = comboBox;
    }

    @Override
    protected void saveColor(Color color) {
        SelfCategory selfCategory = comboBox.getItemAt(comboBox.getSelectedIndex());

        if (selfCategory != null) {
            model.config.setSelfCategoryColor(selfCategory, color);
        }
    }
}
