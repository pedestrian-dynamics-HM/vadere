package org.vadere.gui.onlinevisualization.model;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.AgentColoring;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.onlinevisualization.OnlineVisualization;
import org.vadere.meshing.mesh.gen.AMesh;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.voronoi.VoronoiDiagram;

public class OnlineVisualizationModel extends SimulationModel<DefaultSimulationConfig> {

	/**
	 * Lists for thread safe data exchange between main and draw thread.
	 */
	private LinkedList<VoronoiDiagram> voronoiSnapshots;
	private LinkedList<OnlineVisualization.ObservationAreaSnapshotData> observationAreaSnapshots;

	/**
	 * Latest snapshot of the potential field to be displayed. This is a certain
	 * pontetial field of a certain pedestrian. See 'Simulation' for more
	 * information. For debug purposes. Updated by popDrawData().
	 */
	private IPotentialField potentialFieldTarget = null;

	private IPotentialField potentialField = null;

	private Agent agent = null;

	/**
	 * Latest snapshot of the jts diagram to be displayed. Updated by
	 * popDrawData().
	 */
	private VoronoiDiagram voronoiDiagram = null;

	private double simTimeInSec;

	private boolean drawArrows;

	/**
	 * Synchronizer object used to control access to the simulation data
	 * exchange structures to avoid threading issues.
	 */
	private Object drawDataSynchronizer;


	/**
	 * The observation area to display. Updated by popDrawData() with the latest
	 * observation area snapshot.
	 */
	private Domain domain;

	public OnlineVisualizationModel() {
		super(new DefaultSimulationConfig());
		this.drawDataSynchronizer = new Object();
		this.voronoiSnapshots = new LinkedList<>();
		this.observationAreaSnapshots = new LinkedList<>();
		this.config.setInterpolatePositions(false);
	}

	@Override
	public Collection<Agent> getAgents() {
		if (domain == null || domain.getTopography() == null) {
			return new ArrayList<>();
		}
		Collection<Agent> result = new LinkedList<>();
		result.addAll(domain.getTopography().getElements(Agent.class));
		return result;
	}

	@Override
	public Collection<Pedestrian> getPedestrians() {
		if (domain == null) {
			return new ArrayList<>();
		}
		Collection<Pedestrian> result = new LinkedList<>();
		result.addAll(domain.getTopography().getElements(Pedestrian.class));
		return result;
	}

	@Override
	public int getTopographyId() {
		return 0;
	}

	@Override
	public Topography getTopography() {
		if(domain == null) {
			return null;
		}
		return domain.getTopography();
	}

	@Override
	public Iterator<ScenarioElement> iterator() {
		if (domain == null) {
			return new ArrayList<ScenarioElement>().iterator();
		}
		return new TopographyIterator(domain.getTopography());
	}

	/**
	 * Retrieve latest simulation data from data exchange structures. As these
	 * structures may be accessed by the main thread at the same time, access is
	 * controlled by drawDataSynchronizer.
	 */
	public boolean popDrawData() {
		synchronized (drawDataSynchronizer) {
			if (observationAreaSnapshots.isEmpty()) {
				return false;
			}

			OnlineVisualization.ObservationAreaSnapshotData observationAreaSnapshot =
					observationAreaSnapshots.getFirst();
			simTimeInSec = observationAreaSnapshot.simTimeInSec;

			// potentialFieldTarget might be null!
            potentialFieldTarget = observationAreaSnapshot.potentialFieldTarget;
			potentialField = observationAreaSnapshot.potentialField;
			agent = observationAreaSnapshot.selectedAgent;

			/*
			 * if(topography == null ||
			 * !topography.getBounds().equals(observationAreaSnapshot.scenario.getBounds())) {
			 * setViewportBound(observationAreaSnapshot.scenario.getBounds());
			 * }
			 */

			if (domain == null) {
				domain = observationAreaSnapshot.domain;
				// recalculate GUI (fireChangeViewportEvent will synchronize on model which is also
				// needed by some awt event. Therefore do this in EDT (Event Dispatching Thread)
				EventQueue.invokeLater(() -> {
					fireChangeViewportEvent(new Rectangle2D.Double(getTopography().getBounds().x, getTopography().getBounds().y,
							getTopography().getBounds().width, getTopography().getBounds().height));
				});
			} else {
				domain = observationAreaSnapshot.domain;
			}

			if (getSelectedElement() instanceof Car) {
				int carId = getSelectedElement().getId();
				Car car = domain.getTopography().getElement(Car.class, carId);
				setSelectedElement(car);
			} else if (getSelectedElement() instanceof Pedestrian) {
				int pedId = getSelectedElement().getId();
				Pedestrian ped = domain.getTopography().getElement(Pedestrian.class, pedId);
				setSelectedElement(ped);
			}

			if (isVoronoiDiagramAvailable() && isVoronoiDiagramVisible()) {
				getVoronoiDiagram().computeVoronoiDiagram(domain.getTopography().getPedestrianDynamicElements().getElements()
								.stream()
								.map(ped -> ped.getPosition())
								.collect(Collectors.toList()));
			}

			return true;
		}
	}

	public void pushObservationAreaSnapshot(final OnlineVisualization.ObservationAreaSnapshotData observationAreaSnapshotData) {
        if (observationAreaSnapshots.size() > 0) {
            observationAreaSnapshots.pop();
        }
        observationAreaSnapshots.push(observationAreaSnapshotData);
        setChanged();
	}

	public void reset() {
		voronoiSnapshots.clear();
		observationAreaSnapshots.clear();
		selectedElement = null;

		voronoiDiagram = null;
		domain = null;
		simTimeInSec = 0.0;
	}

	/**
	 * Returns the data synchronization object, used for thread safe data
	 * exchange between main thread and draw thread. Ensure that there is no
	 * simultaneous access to the shared data structures.
	 */
	public Object getDataSynchronizer() {
		return drawDataSynchronizer;
	}

	/**
	 * Returns the list of jts diagram snapshots. Used for thread safe data
	 * exchange between main thread and draw thread.
	 */
	public LinkedList<VoronoiDiagram> getVoronoiSnapshots() {
		return voronoiSnapshots;
	}

	@Override
	public Function<IPoint, Double> getPotentialField() {
	    Function<IPoint, Double> f = pos -> 0.0;

	    if(agent != null && potentialField != null && config.isShowPotentialField() && agent.equals(getSelectedElement())) {
	    	f = pos -> potentialField.getPotential(pos, agent);
	    }
		else if(potentialFieldTarget != null && config.isShowTargetPotentialField()) {
            if(getSelectedElement() instanceof Agent) {
                Agent selectedAgent = (Agent)getSelectedElement();
                f = pos -> potentialFieldTarget.getPotential(pos, selectedAgent);
            }
        }

		return f;
	}

	@Override
	public IMesh<?, ?, ?> getFloorFieldMesh() {
		if(domain.getFloorFieldMesh() != null) {
			return domain.getFloorFieldMesh();
		} else {
			return new AMesh();
		}
	}

	@Override
	public double getGridResolution() {
		return config.getGridWidth();
	}

	@Override
	public boolean isFloorFieldAvailable() {
		return true;
	}

	public double getSimTimeInSec() {
		return simTimeInSec;
	}

	@Override
	public void setAgentColoring(@NotNull AgentColoring agentColoring) {
		switch (agentColoring) {
			case TARGET:
			case GROUP:
			case RANDOM:
			case SELF_CATEGORY:
			case HEALTH_STATUS:
				config.setAgentColoring(agentColoring);
				break;
			case INFORMATION_STATE:
				config.setAgentColoring(agentColoring);
				break;
			default:
				throw new IllegalArgumentException(agentColoring + " is not supported for the online simulation.");
		}
	}

	@Override
	public boolean isAlive(int pedId) {
		return domain.getTopography().getPedestrianDynamicElements().idExists(pedId);
	}
}
