package org.vadere.gui.postvisualization.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

// TODO [priority=low] [task=refactoring] - no strict separation between control and view
public class EJSliderAction implements MouseListener {
	private static Logger logger = LogManager.getLogger(EJSliderAction.class);
	private final JSlider slider;

	public EJSliderAction(final JSlider slider) {
		this.slider = slider;
	}

	@Override
	public void mousePressed(final MouseEvent e) {

	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		Point p = e.getPoint();
		double percent = p.x / ((double) slider.getWidth());
		int range = slider.getMaximum() - slider.getMinimum();
		double newVal = range * percent;
		int result = (int) (Math.ceil(slider.getMinimum() + newVal));
		// logger.info("change to step: " + Thread.currentThread().getName() + (result+1));
		slider.setValue(result);
	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}
}
