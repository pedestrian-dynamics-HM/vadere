package org.vadere.gui.onlinevisualization;

import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.gui.onlinevisualization.model.OnlineVisualizationModel;
import org.vadere.gui.onlinevisualization.view.MainPanel;
import org.vadere.gui.onlinevisualization.view.OnlineVisualisationWindow;
import org.vadere.simulator.control.PassiveCallback;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;

public class OnlineVisualization implements PassiveCallback {

	/**
	 * Holds a snapshot of the observation area of a frame. This class is used
	 * to provide simulation data for visualization to the draw thread. To avoid
	 * threading issues, the class holds a partial copy of the original
	 * scenario.
	 */
	public class ObservationAreaSnapshotData {
		public final double simTimeInSec;
		public final Topography scenario;
		public final IPotentialField potentialFieldTarget;
		public final Agent selectedAgent;
		public final IPotentialField potentialField;

		public ObservationAreaSnapshotData(
				final double simTimeInSec,
				@NotNull final Topography scenario,
				@Nullable final IPotentialField potentialFieldTarget,
				@Nullable final IPotentialField potentialField,
				@Nullable final Agent selectedAgent) {
			this.simTimeInSec = simTimeInSec;
			this.scenario = scenario;
			this.potentialFieldTarget = potentialFieldTarget;
			this.potentialField = potentialField;
			this.selectedAgent = selectedAgent;
		}
	}

	private MainPanel window;
	private OnlineVisualisationWindow onlineVisualisationPanel;
	private OnlineVisualizationModel model;
	private Topography scenario;

	/**
	 * Target potential.
	 */
	private @Nullable IPotentialFieldTarget potentialFieldTarget;

	/**
	 * The overall potential.
	 */
	private @Nullable IPotentialField potentialField;

	private boolean enableVisualization;

	public OnlineVisualization(boolean enableVisualization) {
		this.enableVisualization = enableVisualization;
		this.model = new OnlineVisualizationModel();

		this.window = new MainPanel(model);
		this.window.setVisible(enableVisualization);
		this.onlineVisualisationPanel = new OnlineVisualisationWindow(window, model);
		// this.window.addSelectShapeListener(onlineVisualisationPanel);
	}

	@Override
	public void setTopography(final Topography scenario) {
		this.scenario = scenario;
	}

    @Override
	public void setPotentialFieldTarget(final IPotentialFieldTarget potentialFieldTarget) {
	    this.potentialFieldTarget = potentialFieldTarget;
    }

	@Override
	public void setPotentialField(final IPotentialField potentialField) {
		this.potentialField = potentialField;
	}

	@Override
	public void preLoop(double simTimeInSec) {
		onlineVisualisationPanel.setVisible(this.enableVisualization);
		window.preLoop();
	}

	@Override
	public void postLoop(double simTimeInSec) {
		onlineVisualisationPanel.setVisible(false);
		model.reset();
	}

	@Override
	public void preUpdate(double simTimeInSec) {}

	@Override
	public void postUpdate(double simTimeInSec) {
		pushDrawData(simTimeInSec);
		this.model.notifyObservers();
	}

	/**
	 * Pushes (by copy) required data from current simulation into data queues
	 * for being displayed by draw thread (thread-safe). These may be for
	 * example the physical world representation and potential field of
	 * perception.
	 */
	private void pushDrawData(double simTimeInSec) {

		synchronized (model.getDataSynchronizer()) {
			/* Push new snapshot of the observation area to the draw thread. */
			IPotentialField pft = (model.config.isShowTargetPotentialField() && potentialFieldTarget != null) ? potentialFieldTarget.getSolution() : null;
			IPotentialField pedPotentialField = null;
			Agent selectedAgent = null;

			if(model.config.isShowPotentialField() && model.getSelectedElement() instanceof Agent && potentialField != null) {
				selectedAgent = (Agent)model.getSelectedElement();
				pedPotentialField = IPotentialField.copyAgentField(potentialField, selectedAgent, new VRectangle(model.getTopographyBound()), 0.1);
			}

			ObservationAreaSnapshotData data = new ObservationAreaSnapshotData(simTimeInSec, scenario.clone(), pft, pedPotentialField, selectedAgent);
			model.pushObservationAreaSnapshot(data);
		}
	}


	public JPanel getVisualizationPanel() {
		return onlineVisualisationPanel;
	}

	public MainPanel getMainPanel() {
		return window;
	}
}
