package org.vadere.gui.postvisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.postvisualization.control.ActionSetTimeStep;
import org.vadere.gui.postvisualization.control.EJSliderAction;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

public class AdjustPanel extends JPanel implements Observer {

	// Member Variables
	private final JSlider slider;

	private final JSpinner sVelocity;
	private final JSpinner sTime;
	private final JSpinner sStep;
	private final JSpinner sTimeResolution;

	private final SpinnerModel sModelVelocity;
	// No "final" because if step size of "sTimeResolution" changes,
	// this should also effect the step size of "sTime" and its underlying spinner model.
	private SpinnerModel sModelTime;
	private final SpinnerModel sModelTimeStep;
	private final SpinnerModel sModelTimeResolution;

	private final JLabel lblVelocity;
	private final JLabel lblTime;
	private final JLabel lblStep;
	private final JLabel lblTimeResolution;

	private final PostvisualizationModel model;

	// Constructtors
	public AdjustPanel(final PostvisualizationModel model) {
		this.model = model;

		if (!model.isEmpty()) {
			slider = new JSlider(SwingConstants.HORIZONTAL, model.getFirstStep(),
					model.getLastStep(), model.getFirstStep());
		} else {
			slider = new JSlider(SwingConstants.HORIZONTAL, 0, 0, 0);
		}

		slider.addMouseListener(new EJSliderAction(slider));

		sModelVelocity = new SpinnerNumberModel(model.config.getFps(), 1, 200, 1);
		sModelTime = new SpinnerNumberModel(0.0, 0.0, Double.MAX_VALUE, model.getTimeResolution());
		sModelTimeStep = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1);
		sModelTimeResolution = new SpinnerNumberModel(model.config.getTimeResolution(), 0.01, Double.MAX_VALUE, 0.01);
		model.setTimeResolution(model.config.getTimeResolution());

		sVelocity = new JSpinner(sModelVelocity);
		sTime = new JSpinner(sModelTime);
		sStep = new JSpinner(sModelTimeStep);
		sTimeResolution = new JSpinner(sModelTimeResolution);

		sStep.setPreferredSize(new Dimension(50, 30));
		sVelocity.setPreferredSize(new Dimension(50, 30));
		sTime.setPreferredSize(new Dimension(70, 30));
		sTimeResolution.setPreferredSize(new Dimension(70, 30));

		String labelTemplate = "%s [%s]:";
		lblVelocity = new JLabel(String.format(labelTemplate, Messages.getString("AdjustPanel.lblVelocity.text"), "fps"));
		lblTime = new JLabel(String.format(labelTemplate, Messages.getString("AdjustPanel.lblTime"), "s"));
		lblStep = new JLabel(String.format(labelTemplate, Messages.getString("AdjustPanel.lblStep.text"), "-"));
		lblTimeResolution = new JLabel(String.format(labelTemplate, Messages.getString("AdjustPanel.lblTimeResolution.text"), "s"));

		// Arrange the GUI components according to a column-based layout
		FormLayout layout = new FormLayout(
				"2dlu, default:grow, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu", // col
				"2dlu, default, 2dlu"); // rows
		setLayout(layout);

		CellConstraints cc = new CellConstraints();
		add(slider, cc.xy(2, 2));
		add(lblVelocity, cc.xy(4, 2));
		add(sVelocity, cc.xy(6, 2));
		add(lblTime, cc.xy(8, 2));
		add(sTime, cc.xy(10, 2));
		add(lblStep, cc.xy(12, 2));
		add(sStep, cc.xy(14, 2));
		add(lblTimeResolution, cc.xy(16, 2));
		add(sTimeResolution, cc.xy(18, 2));

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

		sTimeResolution.addChangeListener(e -> {
			model.config.setTimeResolution((double) sTimeResolution.getValue());
			model.setTimeResolution(model.config.getTimeResolution());
			model.notifyObservers();

			double currentTimeValue = (double)sModelTime.getValue();
			double newStepSize = model.config.getTimeResolution();
			sModelTime = new SpinnerNumberModel(currentTimeValue, 0.0, Double.MAX_VALUE, newStepSize);
			sTime.setModel(sModelTime);
		});


		ActionSetTimeStep setTimeStepAction = new ActionSetTimeStep("setTimeStep", model);
		slider.addChangeListener(setTimeStepAction);
		setToolTips();
	}

	private void setToolTips() {
		String unitFramesText = String.format("%s: [%s]",
				Messages.getString("Units.title"),
				Messages.getString("Units.fps"));

		lblVelocity.setToolTipText(unitFramesText);
		sVelocity.setToolTipText(unitFramesText);

		String unitTimeText = String.format("%s: [%s]",
				Messages.getString("Units.title"),
				Messages.getString("Units.time"));

		lblTime.setToolTipText(unitTimeText);
		sTime.setToolTipText(unitTimeText);

		lblTimeResolution.setToolTipText(unitTimeText);
		sTimeResolution.setToolTipText(unitTimeText);

		String unitSimStepText = String.format("%s: [%s]",
				Messages.getString("Units.title"),
				Messages.getString("Units.simStep"));

		lblStep.setToolTipText(unitSimStepText);
		sStep.setToolTipText(unitSimStepText);
	}

	@Override
	public void update(Observable o, Object arg) {
		SwingUtilities.invokeLater(() -> {
			synchronized (model) {
				// update view
				//slider.setValueIsAdjusting(true);
				if(model.hasOutputChanged()) {
					slider.setMaximum(model.getLastStep());
					slider.setMinimum(model.getFirstStep());
				}
				int currentStepNumber = model.getStep();
				slider.setValue(currentStepNumber);
				sStep.setValue(currentStepNumber);
				sTime.setValue(model.getSimTimeInSec());
				sTimeResolution.setValue(model.getTimeResolution());
				sTime.setValue(model.getSimTimeInSec());
				//((SpinnerNumberModel)sModelTime).setStepSize(model.getSimTimeInSec());
				//slider.setValueIsAdjusting(false);
			}
		});
	}
}
