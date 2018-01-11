package org.vadere.gui.onlinevisualization.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author Benedikt Zoennchen
 */
public class ActionShowPotentialField extends AbstractAction implements IRendererChangeListener {
    private static Logger logger = LogManager.getLogger(ActionShowPotentialField.class);
    private final SimulationModel<? extends DefaultSimulationConfig> model;

    public ActionShowPotentialField(final String name, final Icon icon, final SimulationModel<? extends DefaultSimulationConfig> model) {
        super(name, icon);
        this.model = model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        model.config.setShowPotentialField(!model.config.isShowPotentialField());
        model.notifyObservers();
    }

    @Override
    public void update(SimulationRenderer renderer) {}
}
