package org.vadere.gui.onlinevisualization;

import javax.swing.JPanel;

import org.vadere.gui.onlinevisualization.model.OnlineVisualizationModel;
import org.vadere.gui.onlinevisualization.view.MainPanel;
import org.vadere.gui.onlinevisualization.view.OnlineVisualisationWindow;
import org.vadere.gui.onlinevisualization.view.OnlinevisualizationRenderer;
import org.vadere.simulator.control.PassiveCallback;
import org.vadere.state.scenario.Topography;

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

		public ObservationAreaSnapshotData(double simTimeInSec, Topography scenario) {
			this.simTimeInSec = simTimeInSec;
			this.scenario = scenario;
		}
	}

	private MainPanel window;
	private OnlineVisualisationWindow onlineVisualisationPanel;
	private OnlineVisualizationModel model;
	private Topography scenario;
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
			model.pushObersavtionAreaSnapshot(
					new ObservationAreaSnapshotData(simTimeInSec, scenario.clone()));
		}
	}


	public JPanel getVisualizationPanel() {
		return onlineVisualisationPanel;
	}

	public MainPanel getMainPanel() {
		return window;
	}
}
