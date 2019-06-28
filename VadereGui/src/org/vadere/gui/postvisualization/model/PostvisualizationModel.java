package org.vadere.gui.postvisualization.model;

import com.fasterxml.jackson.databind.JsonNode;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.postvisualization.control.TableListenerLogicExpression;
import org.vadere.gui.postvisualization.utils.PotentialFieldContainer;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.TopographyIterator;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.io.parser.VPredicate;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostvisualizationModel extends SimulationModel<PostvisualizationConfig> {

	private static Logger logger = Logger.getLogger(PostvisualizationModel.class);

	//private Step step;

	//private double ratio;

	private double visTime;

	private double visTimeStepLength;

	private double simTimeStepLength;

	private int topographyId;

	private Scenario vadere;

	private PotentialFieldContainer potentialContainer;

	private final PedestrianColorTableModel pedestrianColorTableModel;

	private final Map<Integer, VPredicate<JsonNode>> colorEvalFunctions;

	private Map<Integer, Trajectory> trajectories;

	private Map<Step, List<Agent>> agentsByStep;

	private Comparator<Step> stepComparator = Comparator.comparingInt(Step::getStepNumber);

	private List<Step> steps;

	private String outputPath;

	// public Configuration config;

	public PostvisualizationModel() {
		super(new PostvisualizationConfig());
		this.trajectories = new HashMap<>();
		this.agentsByStep = new HashMap<>();
		this.vadere = new Scenario("");
		this.topographyId = 0;
		this.colorEvalFunctions = new HashMap<>();
		this.potentialContainer = null;
		this.pedestrianColorTableModel = new PedestrianColorTableModel();
		this.steps = new ArrayList<>();
		this.simTimeStepLength = new AttributesSimulation().getSimTimeStepLength();
		this.visTimeStepLength = this.simTimeStepLength;
		this.visTime = 0;
		/*for (int i = 0; i < 5; i++) {
			try {
				colorEvalFunctions.put(i, new JsonLogicParser("false").parse());
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}*/

		this.pedestrianColorTableModel.addTableModelListener(new TableListenerLogicExpression(this, pedestrianColorTableModel));
	}

	public void putExpression(final int row, @NotNull final VPredicate<JsonNode> predicate) {
		colorEvalFunctions.put(row, predicate);
	}

	public void removeExpression(final int row) {
		colorEvalFunctions.remove(row);
	}

	/**
	 * Initialize the {@link PostvisualizationModel}.
	 *
	 * @param agentsByStep  the trajectory information: a list of agent (their position, target, group...) sorted by the time step.
	 * @param scenario      the scenario which was used to produce the output the PostVis will display.
	 * @param projectPath   the path to the project.
	 */
	public void init(final Map<Step, List<Agent>> agentsByStep, final Scenario scenario, final String projectPath) {
		simTimeStepLength = scenario.getAttributesSimulation().getSimTimeStepLength();
		logger.info("start the initialization of the PostvisualizationModel.");
		init(scenario, projectPath);
		this.agentsByStep = agentsByStep;
		trajectories = new HashMap<>();

		// to have fast access to the key values.
		Map<Integer, Step> map = agentsByStep
				.keySet().stream()
				.collect(Collectors.toMap(s -> s.getStepNumber(), s -> s));


		// fill in missing steps by taking the pedestrian of the nearest step smaller than the
		// missing one.
		Optional<Step> optLastStep = map.values().stream().max(Step::compareTo);

		if (optLastStep.isPresent()) {
			for (int stepNumber = 1; stepNumber <= optLastStep.get().getStepNumber(); stepNumber++) {
				if (map.containsKey(stepNumber)) {
					steps.add(map.get(stepNumber));

					for(Agent agent : agentsByStep.get(map.get(stepNumber))) {
						if(!trajectories.containsKey(agent.getId())) {
							trajectories.put(agent.getId(), new Trajectory(agent.getId(), simTimeStepLength));
						}
						trajectories.get(agent.getId()).addStep(map.get(stepNumber), agent);
					}
				} else {
					steps.add(new Step(stepNumber));
				}
			}
		}

		for(Trajectory trajectory : trajectories.values()) {
			trajectory.fill();
		}

		this.visTime = 0;
		logger.info("finished init postvis model");
	}

	private double stepToTime(final int step) {
		return visTimeStepLength * (step - 1);
	}

	public void init(final Scenario vadere, final String projectPath) {
		this.vadere = vadere;
		this.agentsByStep = new HashMap<>();
		this.steps = new ArrayList<>();
		this.trajectories = new HashMap<>();
		this.selectedElement = null;
		this.outputPath = projectPath;
	}

	public double getVisTimeStepLength() {
		return visTimeStepLength;
	}

	public synchronized void setVisTimeStepLength(final double visTimeStepLength) {
		this.visTimeStepLength = visTimeStepLength;
	}

	public double getSimTimeStepLength() {
		return simTimeStepLength;
	}

	public Optional<Step> getLastStep() {
		if (!steps.isEmpty()) {
			return Optional.of(steps.get(steps.size() - 1));
		} else {
			return Optional.empty();
		}
	}

	public Optional<Step> getFirstStep() {
		if (!steps.isEmpty()) {
			return Optional.of(steps.get(0));
		} else {
			return Optional.empty();
		}
	}

	public Step fist() {
		return getFirstStep().get();
	}

	public Step last() {
		return getLastStep().get();
	}

	public Scenario getScenarioRunManager() {
		return vadere;
	}

	@Override
	public Function<IPoint, Double> getPotentialField() {
        Function<IPoint, Double> f = p -> 0.0;
        try {
            if (potentialContainer != null) {
                final CellGrid potentialField = potentialContainer.getPotentialField(Step.toFloorStep(getSimTimeInSec(), getSimTimeStepLength()).getStepNumber());
                f = potentialField.getInterpolationFunction();
            }
        } catch (IOException e) {
            logger.warn("could not load potential field from file.");
            e.printStackTrace();
        }
        return f;
	}

	@Override
	public boolean isFloorFieldAvailable() {
		return potentialContainer != null;
	}

	@Override
	public Collection<Agent> getAgents() {
		return getAlivePedestrians().map(t -> t.getAgent(getSimTimeInSec())).filter(ped -> ped.isPresent()).map(ped -> ped.get())
				.collect(Collectors.toList());
	}

	@Override
	public Topography getTopography() {
		return vadere.getTopography();
	}

	@Override
	public synchronized Iterator<ScenarioElement> iterator() {
		return new TopographyIterator(vadere.getTopography(), getAgents());
	}

	@Override
	public double getGridResolution() {
		return config.getGridWidth();
	}

	/*public synchronized Optional<Step> getStep() {
		return Optional.ofNullable(step);
	}

	public synchronized Optional<Double> getRatio() {
		return Optional.ofNullable(ratio);
	}*/

	public int getStepCount() {
		return steps.size();
	}

	public synchronized void setVisTime(final double visTimeInSec) {
		if(!isEmpty()) {
			double validVisTime = Math.min(Step.toSimTimeInSec(last(), simTimeStepLength), Math.max(Step.toSimTimeInSec(fist(), simTimeStepLength), visTimeInSec));
			if(this.visTime != validVisTime) {
				this.visTime = validVisTime;

				if (isVoronoiDiagramAvailable() && isVoronoiDiagramVisible()) {
					synchronized(getVoronoiDiagram()) {
						getVoronoiDiagram().computeVoronoiDiagram(
								trajectories.values().stream()
										.filter(t -> t.isAlive(visTime))
										.map(t -> t.getAgent(visTime).get().getPosition())
										.collect(Collectors.toList()));
					}
				}

				if (isElementSelected() && getSelectedElement() instanceof Pedestrian) {
					Trajectory trajectory = trajectories.get(getSelectedElement().getId());
					if (trajectory != null) {
						Optional<Agent> ped = trajectory.getAgent(visTime);
						setSelectedElement(ped.orElse(null));
					}
				}

				// so the new pedestrian position is displayed!
				if (isElementSelected()) {
					notifySelectSecenarioElementListener(getSelectedElement());
				}

				setChanged();
			}
		}
	}

	private boolean parseIgnoreException(@NotNull final VPredicate<JsonNode> predicate, @NotNull final JsonNode node) {
		try {
			return predicate.test(node);
		} catch (ParseException e) {
			return false;
		}
	}

	public Optional<Color> getColorByPredicate(final Agent agent) {
		JsonNode jsonObj = StateJsonConverter.toJsonNode(agent);
		Optional<Map.Entry<Integer, VPredicate<JsonNode>>> firstEntry = colorEvalFunctions.entrySet()
				.stream()
				.filter(entry -> parseIgnoreException(entry.getValue(), jsonObj))
				.findFirst();

		if (firstEntry.isPresent()) {
			return Optional.of((Color) pedestrianColorTableModel.getValueAt(firstEntry.get().getKey(),
					PedestrianColorTableModel.COLOR_COLUMN));
		}

		return Optional.empty();
	}

	public PedestrianColorTableModel getPedestrianColorTableModel() {
		return pedestrianColorTableModel;
	}

	/*public synchronized void setStep(final double visTimeInSec) {
		this.visTime = visTimeInSec;
		if (isVoronoiDiagramAvailable() && isVoronoiDiagramVisible()) {
			synchronized(getVoronoiDiagram()) {
				getVoronoiDiagram().computeVoronoiDiagram(
						trajectories.values().stream()
								.filter(t -> t.isAlive(visTimeInSec))
								.map(t -> t.getAgent(visTimeInSec).get().getPosition())
								.collect(Collectors.toList()));
			}
		}

		if (isElementSelected() && getSelectedElement() instanceof Pedestrian) {
			Trajectory trajectory = trajectories.get(getSelectedElement().getId());
			if (trajectory != null) {
				Optional<Agent> ped = trajectory.getAgent(visTimeInSec);
				setSelectedElement(ped.orElse(null));
			}
		}

		// so the new pedestrian position is displayed!
		if (isElementSelected()) {
			notifySelectSecenarioElementListener(getSelectedElement());
		}

		setChanged();

		/*Optional<Step> optionalStep = steps.size() >= step && steps.get(step - 1).getStepNumber() == step
				? Optional.of(steps.get(step - 1)) : Optional.<Step>empty();

		if (!optionalStep.isPresent()) {
			optionalStep = steps.stream().filter(s -> s.getStepNumber() <= step).max(stepComparator);
		}

		if (!optionalStep.isPresent()) {
			logger.error("could not found step with the number: " + step);
		} else if (this.step == null || (!this.step.equals(optionalStep.get()) || ratio != this.ratio)) {

			// cache step and ratio
			this.step = optionalStep.get();
			this.ratio = ratio;
			this.visTime = this.step.getSimTimeInSec() + ratio * getSimTimeStepLength();

			logger.info("calculated time = " + visTime + "," + System.currentTimeMillis());
			int istep = ratio == 0 ? this.step.getStepNumber() : this.step.getStepNumber() + 1;

			Step nextStep = new Step(istep, stepToTime(istep));
			this.ratio = ratio;
			if (isVoronoiDiagramAvailable() && isVoronoiDiagramVisible()) {
				synchronized(getVoronoiDiagram()) {
					getVoronoiDiagram().computeVoronoiDiagram(
							trajectories.values().stream()
									.filter(t -> t.isAlive(nextStep))
									.map(t -> t.getAgent(this.step, ratio).get().getPosition())
									.collect(Collectors.toList()));
				}
			}

			if (isElementSelected() && getSelectedElement() instanceof Pedestrian) {
				Trajectory trajectory = trajectories.get(getSelectedElement().getId());
				if (trajectory != null) {
					Optional<Agent> ped = trajectory.getAgent(this.step, ratio);
					setSelectedElement(ped.orElseGet(() -> null));
				}
			}


			if (isElementSelected()) {
				notifySelectSecenarioElementListener(getSelectedElement());
			}

			setChanged();
		}
	}*/

	public synchronized void setStep(final int step) {
		setVisTime(Step.toSimTimeInSec(new Step(step), simTimeStepLength));
	}

	public synchronized Step getStep() {
		return Step.toFloorStep(getSimTimeInSec(), simTimeStepLength);
	}

	@Override
	public double getSimTimeInSec() {
		return visTime;
	}

	public synchronized void setPotentialFieldContainer(final PotentialFieldContainer container) {
		this.potentialContainer = container;
	}

	public double getMaxSimTimeInSec() {
		return Step.toSimTimeInSec(last(), simTimeStepLength);
	}

	/**
	 * Returns all trajectories. E.g. also trajectories from pedestrian that are not already
	 * appeared and pedestrians that are already reach their target
	 * 
	 * @return all trajectories
	 */
	public synchronized Stream<Trajectory> getAppearedPedestrians() {
		return trajectories.values().stream().filter(t -> t.hasAppeared(getSimTimeInSec()));
	}

	/**
	 * Returns all trajectories of pedestrians that are visible in the topography at the current
	 * time step e. g. they didn't reach their target and they already appear.
	 * 
	 * @return all trajectories of pedestrians that are visible at the current time step
	 */
	public synchronized Stream<Trajectory> getAlivePedestrians() {
		return trajectories.values().stream().filter(t -> t.isAlive(getSimTimeInSec()));
	}

	public boolean isEmpty() {
		return agentsByStep.isEmpty();
	}

	@Override
	public int getTopographyId() {
		return topographyId;
	}

	public String getOutputPath() {
		return outputPath;
	}

	private void clear() {
		setVoronoiDiagram(null);

		if (isFloorFieldAvailable()) {
			try {
				potentialContainer.clear();
			} catch (IOException e) {
				logger.error("could not clear potential Container: " + e.getMessage());
				e.printStackTrace();
			}
		}
		config.setShowTargetPotentialField(false);
		potentialContainer = null;
	}
}
