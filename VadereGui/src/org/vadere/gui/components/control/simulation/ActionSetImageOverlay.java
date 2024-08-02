package org.vadere.gui.components.control.simulation;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class ActionSetImageOverlay extends ActionVisualization implements IRendererChangeListener, ListSelectionListener {
    private final JList<String> jList;

    public ActionSetImageOverlay(final String name, final SimulationModel<? extends DefaultSimulationConfig> model,
                                 final JList<String> comboBox) {
        super(name, model);
        this.jList = comboBox;
    }



    @Override
    public void actionPerformed(final ActionEvent event) {

        System.out.println("action Performed:");

        String imageName = jList.getSelectedValuesList().get(0);
        System.out.println(imageName);

        BufferedImage image;
        try {
            image = ImageIO.read(Resources.class.getResource("/agent_icons/" + imageName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        model.config.setImage(image);
        model.notifyObservers();
        super.actionPerformed(event);

    }

    @Override
    public void update(SimulationRenderer renderer) {
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {


        List<String> images = jList.getSelectedValuesList();

        System.out.println("Value changed:");
        for(String image: images){
            System.out.println(image);
        }


        String imageName = jList.getSelectedValue();
        System.out.println(imageName);

        BufferedImage image;
        try {
            image = ImageIO.read(Resources.class.getResource("/agent_icons/" + imageName));
        } catch (IOException event) {
            throw new RuntimeException(event);
        }
        model.config.setImage(image);
        model.notifyObservers();

        System.out.println("Value changed FINISHED");

    }
}
