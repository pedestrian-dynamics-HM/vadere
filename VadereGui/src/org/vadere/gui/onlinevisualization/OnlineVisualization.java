package org.vadere.gui.onlinevisualization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.gui.onlinevisualization.model.OnlineVisualizationModel;
import org.vadere.gui.onlinevisualization.view.MainPanel;
import org.vadere.gui.onlinevisualization.view.OnlineVisualisationWindow;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.simulator.control.simulation.PassiveCallback;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.function.Function;

public class OnlineVisualization implements PassiveCallback {

	/**
	 * Holds a snapshot of the observation area of a frame. This class is used
	 * to provide simulation data for visualization to the draw thread. To avoid
	 * threading issues, the class holds a partial copy of the original
	 * scenario.
	 */
	public class ObservationAreaSnapshotData {
		public final double simTimeInSec;
		public final Domain domain;
		public final IPotentialField potentialFieldTarget;
		public final Agent selectedAgent;
		public final IPotentialField potentialField;
		public final Function<Agent, IMesh<?, ?, ?>> discretizations;

		public ObservationAreaSnapshotData(
				final double simTimeInSec,
				@NotNull final Domain scenario,
				@Nullable final IPotentialField potentialFieldTarget,
				@Nullable final IPotentialField potentialField,
				@Nullable final Agent selectedAgent,
				@Nullable final Function<Agent, IMesh<?, ?, ?>> discretizations) {
			this.simTimeInSec = simTimeInSec;
			this.domain = scenario;
			this.potentialFieldTarget = potentialFieldTarget;
			this.potentialField = potentialField;
			this.selectedAgent = selectedAgent;
			this.discretizations = discretizations;
		}
	}

	private MainPanel window;
	private OnlineVisualisationWindow onlineVisualisationPanel;
	private OnlineVisualizationModel model;
	private Domain domain;

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
	public void setDomain(final Domain domain) {
		this.domain = domain;
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
		// [issue 280] ensure OnlineVisualisation model is completely setup before
		// OnlineVisualisation renderer is initialized in window.preLoop()
		// push pop DrawData once at the beginning. This will completely initialize the model
		// (i.e. set Topography to correct value. Before this call it is null....)
		pushDrawData(simTimeInSec);
		model.popDrawData();

		// [issue 280] activate mouse listeners to allow zoom action in OnlineVisualisation
		window.addListener();
		onlineVisualisationPanel.setVisible(this.enableVisualization);
		window.preLoop();
	}

	@Override
	public void postLoop(double simTimeInSec) {
		onlineVisualisationPanel.setVisible(false);
		model.reset();

		// [issue 280] deactivate mouse listeners because model is not valid anymore
		window.removeListeners();
	}

	@Override
	public void preUpdate(double simTimeInSec) {}

	@Override
	public void postUpdate(double simTimeInSec) {
		pushDrawData(simTimeInSec);
		model.popDrawData();
		model.notifyObservers();
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
			Function<Agent, IMesh<?, ?, ?>> discretizations = (model.config.isShowTargetPotentielFieldMesh() && potentialFieldTarget != null) ? potentialFieldTarget.getDiscretization() : null;
			IPotentialField pedPotentialField = null;
			Agent selectedAgent = null;

			if(model.getSelectedElement() instanceof Agent){
				selectedAgent = (Agent)model.getSelectedElement();
			}

			if(model.config.isShowPotentialField() && selectedAgent != null && potentialField != null) {
				pedPotentialField = IPotentialField.copyAgentField(potentialField, selectedAgent, new VRectangle(model.getTopographyBound()), 0.1);
			}

			ObservationAreaSnapshotData data = new ObservationAreaSnapshotData(simTimeInSec, domain.clone(), pft, pedPotentialField, selectedAgent, discretizations);
			model.pushObservationAreaSnapshot(data);
		}
	}


	// [issue 280] show OnlineVisualization Window and remove mouse Listeners. This is necessary to ensure
	// that no null pointer exception is thrown in the awt thread due to not completely
	// initialized OnlineVisualisation model. A better fix would be to only initialize the
	// OnlineVisualisation AFTER the model is loaded completely but this would need more changes
	// in the gui setup.
	public void showVisualization(){
		window.setVisible(true);
		window.removeListeners();
	}

	public OnlineVisualisationWindow getVisualizationPanel() {
		return onlineVisualisationPanel;
	}

	public MainPanel getMainPanel() {
		return window;
	}
}
