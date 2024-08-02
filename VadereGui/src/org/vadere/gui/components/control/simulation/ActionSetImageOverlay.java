package org.vadere.gui.components.control.simulation;

import org.jetbrains.annotations.NotNull;
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
import java.util.LinkedList;
import java.util.List;

public class ActionSetImageOverlay extends ActionVisualization implements IRendererChangeListener, ListSelectionListener {
    private final JList<String> jList;

    public ActionSetImageOverlay(final String name, final SimulationModel<? extends DefaultSimulationConfig> model,
                                 final JList<String> jList) {
        super(name, model);
        this.jList = jList;
        setInitialImage();
    }

    private void setInitialImage(){

        jList.setSelectedIndex(0);
        List<String> images = jList.getSelectedValuesList();
        LinkedList<BufferedImage> linkedList = getBufferedImageLinkedList(images);
        model.config.setImage(linkedList);
        model.notifyObservers();
    }

    @Override
    public void update(SimulationRenderer renderer) {
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {


        List<String> images = jList.getSelectedValuesList();
        LinkedList<BufferedImage> linkedList = getBufferedImageLinkedList(images);
        model.config.setImage(linkedList);
        model.notifyObservers();


    }

    private static LinkedList<BufferedImage> getBufferedImageLinkedList(List<String> images) {
        BufferedImage image;
        LinkedList<BufferedImage> linkedList = new LinkedList<>();
        for(String imageName: images){
            try {
                image = ImageIO.read(Resources.class.getResource("/agent_icons/" + imageName));
                linkedList.add(image);
            } catch (IOException event) {
                throw new RuntimeException(event);
            }
        }
        return linkedList;
    }
}
