package org.vadere.gui.postvisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.control.ActionSetTimeStep;
import org.vadere.gui.postvisualization.control.EJSliderAction;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.state.simulation.Step;

import java.awt.*;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

public class AdjustPanel extends JPanel implements Observer {
	private static Resources resources = Resources.getInstance("postvisualization");

	private final JSlider slider;
	private final JSpinner sStep;
	private final JSpinner sVelocity;
	private final JSpinner sVisTimeStepLength;

	private final JSpinner sTime;
	private final SpinnerModel sModelTime;
	private final SpinnerModel sModelTimeStep;
	private final SpinnerModel sModelVelocity;

	private final SpinnerModel sModelVisTimeStepLength;
	private final PostvisualizationModel model;

	public AdjustPanel(final PostvisualizationModel model) {
		this.model = model;
		if (model.getFirstStep().isPresent() && model.getLastStep().isPresent()) {
			slider = new JSlider(SwingConstants.HORIZONTAL, model.getFirstStep().get().getStepNumber(),
					model.getLastStep().get().getStepNumber(), model.getFirstStep().get().getStepNumber());
		} else {
			slider = new JSlider(SwingConstants.HORIZONTAL, 1, 1, 1);
		}

		slider.addMouseListener(new EJSliderAction(slider));
		// sStep.setEditable(false);
		sModelVelocity = new SpinnerNumberModel(model.config.getFps(), 1, 200, 1);
		sModelTimeStep = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);
		sModelTime = new SpinnerNumberModel(0.0, 0.0, Double.MAX_VALUE, model.getVisTimeStepLength());
		sModelVisTimeStepLength = new SpinnerNumberModel(model.getVisTimeStepLength(), 0.01, Double.MAX_VALUE, 0.01);

		sVelocity = new JSpinner(sModelVelocity);
		sTime = new JSpinner(sModelTime);
		sStep = new JSpinner(sModelTimeStep);
		sVisTimeStepLength = new JSpinner(sModelVisTimeStepLength);

		//sTime.setEnabled(false);


		// lTime.set
		// lTime.setV
		sStep.setPreferredSize(new Dimension(50, 30));
		sVelocity.setPreferredSize(new Dimension(50, 30));
		sTime.setPreferredSize(new Dimension(70, 30));
		sVisTimeStepLength.setPreferredSize(new Dimension(70, 30));
		// Layout definition!
		FormLayout layout = new FormLayout(
				"2dlu, default:grow, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu", // col
				"2dlu, default, 2dlu"); // rows
		setLayout(layout);
		CellConstraints cc = new CellConstraints();
		add(slider, cc.xy(2, 2));
		add(new JLabel(Messages.getString("AdjustPanel.lblVelocity.text")), cc.xy(4, 2));
		add(sVelocity, cc.xy(6, 2));
		add(new JLabel(Messages.getString("AdjustPanel.lblTime")), cc.xy(8, 2));
		add(sTime, cc.xy(10, 2));
		add(new JLabel(Messages.getString("AdjustPanel.lblStep.text")), cc.xy(12, 2));
		add(sStep, cc.xy(14, 2));
		add(new JLabel(Messages.getString("AdjustPanel.lblVisTime.text")), cc.xy(16, 2));
		add(sVisTimeStepLength, cc.xy(18, 2));

		sVelocity.addChangeListener(e -> {
			model.config.setFps((int) sVelocity.getValue());
			model.notifyObservers();
		});

		sTime.addChangeListener(e -> {
			model.setVisTime((double)sModelTime.getValue());
			model.notifyObservers();
		});

		sStep.addChangeListener(e -> {
			if ((int) sStep.getValue() <= model.getStepCount()) {
				model.setStep((int) sStep.getValue());
				model.notifyObservers();
			}
		});

		sVisTimeStepLength.addChangeListener(e -> {
			model.setVisTimeStepLength((double) sVisTimeStepLength.getValue());
			model.notifyObservers();
		});

		ActionSetTimeStep setTimeStepAction = new ActionSetTimeStep("setTimeStep", model);
		slider.addChangeListener(setTimeStepAction);
	}

	@Override
	public void update(Observable o, Object arg) {
		// update view
		slider.setMaximum(model.getLastStep().map(step -> step.getStepNumber()).orElse(1));
		slider.setMinimum(model.getFirstStep().map(step -> step.getStepNumber()).orElse(1));

		int currentStepNumber = model.getStep().getStepNumber();

		slider.setValue(currentStepNumber);
		sStep.setValue(currentStepNumber);
		sTime.setValue(model.getSimTimeInSec());
		sModelVisTimeStepLength.setValue(model.getVisTimeStepLength());
		((SpinnerNumberModel)sModelTime).setStepSize(model.getVisTimeStepLength());
	}
}
