package org.vadere.gui.components.control.simulation;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.onlinevisualization.view.IRendererChangeListener;
import org.vadere.gui.postvisualization.view.ComboBoxMultiSelect;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ActionSetImageOverlay extends ActionVisualization implements IRendererChangeListener {

    private static final Logger LOGGER = Logger.getLogger(ActionSetImageOverlay.class);

    private final ComboBoxMultiSelect<String> jList;

    public ActionSetImageOverlay(final String name, final SimulationModel<? extends DefaultSimulationConfig> model,
                                 final ComboBoxMultiSelect<String> jList) {
        super(name, model);
        this.jList = jList;
        initializeImage();

    }

    private void initializeImage() {
        LinkedList<BufferedImage> linkedList = getBufferedImageLinkedList(jList.getSelectedElements());
        model.config.setImage(linkedList);
        model.notifyObservers();
    }

    @Override
    public void update(SimulationRenderer renderer) {
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        Object obj = jList.getSelectedItem();
        if (jList.getSelectedElements().contains(obj)) {
            jList.removeItemObject(obj);
        } else {
            jList.addItemObject(obj);
        }

        List<Object> images = jList.getSelectedElements();
        LinkedList<BufferedImage> linkedList = getBufferedImageLinkedList(images);
        model.config.setImage(linkedList);
        model.notifyObservers();
        super.actionPerformed(e);

    }

    private LinkedList<BufferedImage> getBufferedImageLinkedList(List<Object> images) {
        BufferedImage image;
        LinkedList<BufferedImage> linkedList = new LinkedList<>();

        for(Object imageName: images){
            try {
                File imagePath = new File(model.config.getImageDirectory(), (String) imageName);
                image = ImageIO.read(imagePath);

                checkImageRatio(image, imagePath);


                linkedList.add(image);
            } catch (IOException event) {
                throw new RuntimeException(event);
            }
        }
        return linkedList;
    }

    private void checkImageRatio (BufferedImage image, File imagePath) {
        double h = image.getHeight();
        double w = image.getWidth();
        double aspectRatio =  w/h;
        double aspectRatioAllowed = 1.2;

        if (aspectRatio > aspectRatioAllowed || aspectRatioAllowed < 1/aspectRatioAllowed){
            LOGGER.info("Image " + imagePath.toString() + ": side lengths differ strongly." +
                    "Width = " +  image.getWidth() + ". Height = " + image.getHeight() + "." +
                     " Aspect ratio: " + aspectRatio + ". It is recommended to use quadratic images.");
        }
    }
}
