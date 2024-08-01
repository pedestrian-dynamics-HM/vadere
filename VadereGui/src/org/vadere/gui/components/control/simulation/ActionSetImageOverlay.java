package org.vadere.gui.components.control.simulation;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.information.InformationState;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ActionSetImageOverlay extends ActionVisualization {
    private final JComboBox<String> comboBox;

    public ActionSetImageOverlay(final String name, final SimulationModel<? extends DefaultSimulationConfig> model,
                                 final JComboBox<String> comboBox) {
        super(name, model);
        this.comboBox = comboBox;
    }

    @Override
    public void actionPerformed(final ActionEvent event) {

        String imageName = comboBox.getItemAt(comboBox.getSelectedIndex());

        System.out.println("actionPerformed" + imageName);

        BufferedImage image;
        try {
            image = ImageIO.read(Resources.class.getResource("/agent_icons/" + imageName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        model.config.setImage(image);

        super.actionPerformed(event);
        model.notifyObservers();


    }


}
