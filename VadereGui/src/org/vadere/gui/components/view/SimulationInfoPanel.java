/**
 * Edited to enable the infection transmission behavior
 *  By: Mina Abadeer(1), Sameh Magharious(2)
 *
 * (1)Group Parallel and Distributed Systems
 * Department of Computer Science
 * University of Muenster, Germany
 *
 * (2)Dell Technologies, USA
 *
 * This software is licensed under the GNU Lesser General Public License (LGPL).
 */

package org.vadere.gui.components.view;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

import javax.swing.*;
import java.util.Observable;

public class SimulationInfoPanel extends InfoPanel {

	private final SimulationModel<DefaultSimulationConfig> simModel;

	private JLabel lblSimTime;
	private JLabel lblNumberOfPedestrians;
	private JLabel lblNumberOfInfectedPedestrians;
	private JLabel lblInfectionRate;

	private JLabel lblSimTimeValue;
	private JLabel lblNumberOfPedestriansValue;
	private JLabel lblNumberOfInfectedPedestriansValue;
	private JLabel lblInfectionRateValue;

	public SimulationInfoPanel(final SimulationModel<DefaultSimulationConfig> simModel) {
		super(simModel);
		this.simModel = simModel;

		lblSimTime = new JLabel("SimTimeInSec:");
		lblNumberOfPedestrians = new JLabel("# Ped:");
		lblNumberOfInfectedPedestrians = new JLabel("# Infected Peds:");
		lblInfectionRate = new JLabel("Infection Rate:");

		lblSimTimeValue = new JLabel();
		lblNumberOfPedestriansValue = new JLabel();
		lblNumberOfInfectedPedestriansValue = new JLabel();
		lblInfectionRateValue = new JLabel();

		add(lblSimTime);
		add(lblSimTimeValue);

		add(lblNumberOfPedestrians);
		add(lblNumberOfPedestriansValue);

		add(lblNumberOfInfectedPedestrians);
		add(lblNumberOfInfectedPedestriansValue);

		add(lblInfectionRate);
		add(lblInfectionRateValue);
	}

	@Override
	public void update(Observable o, Object arg) {
		super.update(o, arg);
		lblSimTimeValue.setText(String.format("%3.2f | ", simModel.getSimTimeInSec()));
		lblNumberOfPedestriansValue.setText(String.format("%d |", simModel.getAgents().size()));
		lblNumberOfInfectedPedestriansValue.setText(String.format("%d", simModel.getInfectedPedestrians().size()));
		lblInfectionRateValue.setText(String.format("%3.2f", simModel.getInfectionRate()));
	}
}
