package org.vadere.gui.components.control.simulation;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.gui.postvisualization.view.ComboBoxMultiSelect;

import javax.imageio.ImageIO;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ActionSetImageOverlay extends ActionVisualization implements IRendererChangeListener {
    private final ComboBoxMultiSelect<String> jList;

    public ActionSetImageOverlay(final String name, final SimulationModel<? extends DefaultSimulationConfig> model,
                                 final ComboBoxMultiSelect<String> jList) {
        super(name, model);
        this.jList = jList;

        LinkedList<BufferedImage> linkedList = getBufferedImageLinkedList(jList.getSelectedItems());
        model.config.setImage(linkedList);
        model.notifyObservers();
    }



    @Override
    public void update(SimulationRenderer renderer) {
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        Object obj = jList.getSelectedItem();
        if (jList.getSelectedItems().contains(obj)) {
            jList.removeItemObject(obj);
        } else {
            jList.addItemObject(obj);
        }


        List<Object> images = jList.getSelectedItems();
        System.out.println( "Selected:" + images);

        LinkedList<BufferedImage> linkedList = getBufferedImageLinkedList(images);
        model.config.setImage(linkedList);
        model.notifyObservers();
        super.actionPerformed(e);

    }

    private static LinkedList<BufferedImage> getBufferedImageLinkedList(List<Object> images) {
        BufferedImage image;
        LinkedList<BufferedImage> linkedList = new LinkedList<>();
        for(Object imageName: images){
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
