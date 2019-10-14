package org.vadere.gui.postvisualization.control;


import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;

// TODO [priority=low] [task=refactoring] - no strict separation between control and view
public class EJSliderAction implements MouseListener {
	private static Logger logger = Logger.getLogger(EJSliderAction.class);
	private final JSlider slider;

	public EJSliderAction(final JSlider slider) {
		this.slider = slider;
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		setSliderValue(e);
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		setSliderValue(e);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		setSliderValue(e);
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	private void setSliderValue(MouseEvent e) {
		Point sliderPosition = e.getPoint();
		double percent = sliderPosition.x / ((double) slider.getWidth());
		int sliderRange = slider.getMaximum() - slider.getMinimum();

		double newValue = sliderRange * percent;
		int result = (int) (Math.ceil(slider.getMinimum() + newValue));
		slider.setValue(result);
	}
}
