package org.vadere.gui.postvisualization.control;


import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.util.logging.Logger;

import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ActionSetTimeStep extends ActionVisualization implements ChangeListener {
	private static Logger logger = Logger.getLogger(ActionSetTimeStep.class);
	private PostvisualizationModel model;

	public ActionSetTimeStep(final String name, PostvisualizationModel model) {
		super(name, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() instanceof JTextField) {
			JTextField field = (JTextField) e.getSource();
			try {
				int step = Integer.parseInt(field.getText());
				if (model.getLastStep().isPresent() && model.getFirstStep().isPresent()) {
					if (step <= model.getLastStep().get().getStepNumber()
							&& step >= model.getFirstStep().get().getStepNumber()) {
						model.setStep(step);
						model.notifyObservers();
					}
				}
			} catch (NumberFormatException ex) {
				logger.warn(ex);
			}
		}
		super.actionPerformed(e);
	}

	@Override
	public void stateChanged(final ChangeEvent event) {
		JSlider source = (JSlider) event.getSource();
		// if (!source.getV) {
		model.setStep(source.getValue());
		// logger.info("change to step: " + Thread.currentThread().getName() + (source.getValue() +
		// 1));
		model.notifyObservers();
		// }
	}
}
