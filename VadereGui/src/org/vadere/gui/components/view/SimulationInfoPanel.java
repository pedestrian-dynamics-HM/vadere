package org.vadere.gui.components.view;

import javax.swing.*;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

import java.util.Observable;

public class SimulationInfoPanel extends InfoPanel {

	private final SimulationModel<DefaultSimulationConfig> simModel;

	private JLabel lblSimTimeLabel;
	private JLabel lblNumberOfPedestriansLabel;

	private JLabel lblSimTimeValue;
	private JLabel lblNumberOfPedestriansValue;

	public SimulationInfoPanel(final SimulationModel<DefaultSimulationConfig> simModel) {
		super(simModel);
		this.simModel = simModel;

		lblSimTimeLabel = new JLabel("SimTimeInSec:");
		lblNumberOfPedestriansLabel = new JLabel("#Ped:");

		lblSimTimeValue = new JLabel();
		lblNumberOfPedestriansValue = new JLabel();

		add(lblSimTimeLabel);
		add(lblSimTimeValue);

		add(lblNumberOfPedestriansLabel);
		add(lblNumberOfPedestriansValue);
	}

	@Override
	public void update(Observable o, Object arg) {
		super.update(o, arg);
		lblSimTimeValue.setText(String.format("%3.2f | ", simModel.getSimTimeInSec()));
		lblNumberOfPedestriansValue.setText(String.format("%d", simModel.getAgents().size()));
	}
}
