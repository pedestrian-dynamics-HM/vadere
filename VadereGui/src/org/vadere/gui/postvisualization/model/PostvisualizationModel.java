package org.vadere.gui.postvisualization.model;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.AgentColoring;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.postvisualization.utils.PotentialFieldContainer;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.attributes.AttributesSimulation;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.BasicExposureModelHealthStatus;
import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.perception.types.StimulusFactory;
import org.vadere.state.scenario.*;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

public class PostvisualizationModel extends SimulationModel<PostvisualizationConfig> {

	private static Logger logger = Logger.getLogger(PostvisualizationModel.class);

	private boolean outputChanged;

	//private Step step;

	//private double ratio;

	private double visTime;

	private double timeResolution;

	private double simTimeStepLength;

	private int topographyId;

	private Scenario scenario;

	private PotentialFieldContainer potentialContainer;

	private TableAerosolCloudData tableAerosolCloudData;

	private PredicateColoringModel predicateColoringModel;

	private TableTrajectoryFootStep trajectories;

	private ContactData contactData;

	private String outputPath;

	private AttributesAgent attributesAgent;

	// public Configuration config;

	public PostvisualizationModel() {
		super(new PostvisualizationConfig());
		this.trajectories = new TableTrajectoryFootStep(Table.create());
		this.contactData = new ContactData(Table.create());
		this.scenario = new Scenario("");
		this.topographyId = 0;
		this.potentialContainer = null;
		this.tableAerosolCloudData = new TableAerosolCloudData(Table.create());
		this.simTimeStepLength = new AttributesSimulation().getSimTimeStepLength();
		this.timeResolution = this.simTimeStepLength;
		this.visTime = 0;
		this.predicateColoringModel = new PredicateColoringModel();
		this.outputChanged = false;
	}
	public synchronized void init(final Table trajectories, final HashMap<String, Table> additionalTables, final Scenario scenario, final String projectPath) {
		init(trajectories, additionalTables, scenario, projectPath, new AttributesAgent());
	}

	public synchronized void init(final Table trajectories, final Scenario scenario, final String projectPath) {
		init(trajectories, new HashMap<>(), scenario, projectPath, new AttributesAgent());
	}

	/**
	 * Initialize the {@link PostvisualizationModel}.
	 * @param trajectories
	 * @param additionalTables 	tables containing additional data that is not stored in trajectories but should be
	 *                          postvisualized as well, for example contacts and aerosol clouds
	 * @param scenario      the scenario which was used to produce the output the PostVis will display.
	 *                      This scenario will not contain any agents.
	 * @param projectPath   the path to the project.
	 */
	public synchronized void init(final Table trajectories, final HashMap<String, Table> additionalTables, final Scenario scenario, final String projectPath, final AttributesAgent attributesAgent) {
		this.scenario = scenario;
		this.simTimeStepLength = scenario.getAttributesSimulation().getSimTimeStepLength();
		this.trajectories = new TableTrajectoryFootStep(trajectories);
		clearAdditionalTables();
		for (HashMap.Entry<String, Table> entry : additionalTables.entrySet()) {
			switch (entry.getKey()) {
				case ContactData.TABLE_NAME:
					this.config.setContactsRecorded(true);
					this.contactData = new ContactData(entry.getValue());
				case TableAerosolCloudData.TABLE_NAME:
					this.config.setAerosolCloudsRecorded(true);
					this.tableAerosolCloudData = new TableAerosolCloudData(entry.getValue());
			}
		}
		this.visTime = 0;
		this.attributesAgent = attributesAgent;
		this.outputPath = projectPath;
		this.outputChanged = true;
	}

	/**
	 * Initialize an empty {@link PostvisualizationModel}.
	 *
	 * @param scenario      the scenario which was used to produce the output the PostVis will display.
	 *                      This scenario will not contain any agents.
	 * @param projectPath   the path to the project.
	 */
	public synchronized void init(final Scenario scenario, final String projectPath) {
		this.scenario = scenario;
		this.trajectories = new TableTrajectoryFootStep(Table.create());
		this.selectedElement = null;
		this.outputPath = projectPath;
		this.outputChanged = true;
	}

	private double stepToTime(final int step) {
		return timeResolution * (step - 1);
	}

	public synchronized PredicateColoringModel getPredicateColoringModel() {
		return predicateColoringModel;
	}

	public synchronized double getTimeResolution() {
		return timeResolution;
	}

	public synchronized void setTimeResolution(final double visTimeStepLength) {
		this.timeResolution = visTimeStepLength;
	}

	public synchronized double getSimTimeStepLength() {
		return simTimeStepLength;
	}

	public synchronized int getLastStep() {
		return (int)Math.ceil(trajectories.getMaxEndTime() / getSimTimeStepLength());
	}

	public synchronized int getFirstStep() {
		return (int)Math.floor(trajectories.getMinStartTime() / getSimTimeStepLength());
	}

	public synchronized Scenario getScenarioRunManager() {
		return scenario;
	}

	@Override
	public synchronized Function<IPoint, Double> getPotentialField() {
        Function<IPoint, Double> f = p -> 0.0;
        try {
            if (potentialContainer != null) {
                final CellGrid potentialField = potentialContainer.getPotentialField(Step.toFloorStep(getSimTimeInSec(), getSimTimeStepLength()));
                f = potentialField.getInterpolationFunction();
            }
        } catch (IOException e) {
            logger.warn("could not load potential field from file.");
            e.printStackTrace();
        }
        return f;
	}

	@Override
	public synchronized boolean isFloorFieldAvailable() {
		return potentialContainer != null;
	}

	@Override
	public synchronized Collection<Agent> getAgents() {
		Table agents = getAgentTable();
		List<Agent> agentList = new ArrayList<>(agents.rowCount());
		for(Row agentRow : agents) {
			agentList.add(toAgent(agentRow));
		}
		return agentList;
	}

	@Override
	public Collection<Pedestrian> getPedestrians() {
		Table agents = getAgentTable();
		List<Pedestrian> agentList = new ArrayList<>(agents.rowCount());
		for(Row agentRow : agents) {
			agentList.add(toAgent(agentRow));
		}
		return agentList;
	}

	public synchronized TableTrajectoryFootStep getTrajectories() {
		return trajectories;
	}
	public synchronized ContactData getContactData() {
		return contactData;
	}

	public synchronized Table getAgentTable() {
		return trajectories.getAgentsWithDisappearedAgents(getSimTimeInSec());
	}

	public synchronized TableAerosolCloudData getTableAerosolCloudData() {
		return tableAerosolCloudData;
	}

	public synchronized DoubleColumn getDeathTime() {
		return trajectories.getDeathTime();
	}

	public synchronized Table getAgentDataFrame() {
		return trajectories.getAgentDataFrame();
	}

	private void clearAdditionalTables() {
		config.setContactsRecorded(false);
		contactData = new ContactData(Table.create());
		config.setAerosolCloudsRecorded(false);
		tableAerosolCloudData = new TableAerosolCloudData(Table.create());
	}

	private Pedestrian toAgent(final Row row) {
		int pedId = row.getInt(trajectories.pedIdCol);
		double startTime = row.getDouble(trajectories.startTimeCol);
		double endTime = row.getDouble(trajectories.endTimeCol);
		double startX = row.getDouble(trajectories.startXCol);
		double startY = row.getDouble(trajectories.startYCol);
		double endX = row.getDouble(trajectories.endXCol);
		double endY = row.getDouble(trajectories.endYCol);

		VPoint position;
		if(config.isInterpolatePositions() && (startTime <= getSimTimeInSec() && endTime >= getSimTimeInSec())) {
			position = FootStep.interpolateFootStep(startX, startY, endX, endY, startTime, endTime, getSimTimeInSec());
		} else {
			position = new VPoint(endX, endY);
		}

		Pedestrian pedestrian = new Pedestrian(new AttributesAgent(attributesAgent, pedId), new Random());
		pedestrian.setPosition(position);

		if(trajectories.targetIdCol != -1) {
			pedestrian.getTargets().addLast(row.getInt(trajectories.targetIdCol));
		}

		if(trajectories.groupIdCol != -1) {
			pedestrian.getGroupIds().addLast(row.getInt(trajectories.groupIdCol));
		}

		if(trajectories.groupSizeCol != -1) {
			pedestrian.getGroupSizes().addLast(row.getInt(trajectories.groupSizeCol));
		}

		if(trajectories.mostImportantStimulusCol != -1) {
			String mostImportantStimulusString = row.getString(trajectories.mostImportantStimulusCol);
			pedestrian.setMostImportantStimulus(StimulusFactory.stringToStimulus(mostImportantStimulusString));
		}

		if(trajectories.selfCategoryCol != -1) {
			String selfCategoryString = row.getString(trajectories.selfCategoryCol);
			pedestrian.setSelfCategory(SelfCategory.valueOf(selfCategoryString));
		}

		if (trajectories.informationStateCol != -1){
			String informationStateString = row.getString(trajectories.informationStateCol);
			pedestrian.getKnowledgeBase().setInformationState(InformationState.valueOf(informationStateString));
		}



		if(trajectories.groupMembershipCol != -1) {
			String groupMembershipString = row.getString(trajectories.groupMembershipCol);
			pedestrian.setGroupMembership(GroupMembership.valueOf(groupMembershipString));
		}

		if(trajectories.isInfectiousCol != -1) {
			boolean isInfectiousString = row.getBoolean(trajectories.isInfectiousCol);
			if (pedestrian.getHealthStatus() == null) {
				pedestrian.setHealthStatus(new BasicExposureModelHealthStatus());
			}
			pedestrian.setInfectious(isInfectiousString);
		}

		if(trajectories.degreeOfExposureCol != -1) {
			if (pedestrian.getHealthStatus() == null) {
				pedestrian.setHealthStatus(new BasicExposureModelHealthStatus());
			}
			pedestrian.setDegreeOfExposure(row.getDouble(trajectories.degreeOfExposureCol));
		}

		return pedestrian;
	}

	private VPoint toPosition(final Row row) {
		return new VPoint(row.getDouble(trajectories.endXCol), row.getDouble(trajectories.endYCol));
	}

	@Override
	public synchronized Topography getTopography() {
		return scenario.getTopography();
	}

	@Override
	public synchronized Iterator<ScenarioElement> iterator() {
		return new TopographyIterator(scenario.getTopography(), getAgents());
	}

	@Override
	public synchronized double getGridResolution() {
		return config.getGridWidth();
	}

	public synchronized int getStepCount() {
		return getLastStep() - getFirstStep();
	}

	public synchronized void setVisTime(final double visTimeInSec) {
		if(!isEmpty()) {
			double validVisTime = Math.min(Math.max(trajectories.getMinStartTime(), visTimeInSec), trajectories.getMaxEndTime());

			if(this.visTime != validVisTime) {
				visTime = validVisTime;
				trajectories.setSlice(trajectories.getMinStartTime(), visTime);

				if (isVoronoiDiagramAvailable() && isVoronoiDiagramVisible()) {
					synchronized(getVoronoiDiagram()) {
						getVoronoiDiagram().computeVoronoiDiagram(getPositions());
					}
				}

				if (isElementSelected() && getSelectedElement() instanceof Pedestrian) {
					int pedId = getSelectedElement().getId();
					Table agentTable = trajectories.getAgent(getSimTimeInSec(), pedId);

					if (!agentTable.isEmpty()) {
						Optional<Agent> ped = Optional.ofNullable(toAgent(agentTable.iterator().next()));
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

	public synchronized List<VPoint> getPositions() {
		List<VPoint> positions = new ArrayList<>();
		trajectories.getAgents(getSimTimeInSec()).forEach(row -> positions.add(toPosition(row)));
		return positions;
	}

	public synchronized void setStep(final int step) {
		if(getStep() != step) {
			setVisTime(Step.toSimTimeInSec(step, getSimTimeStepLength()));
		}
	}

	public synchronized int getStep() {
		return Step.toCeilStep(getSimTimeInSec(), getSimTimeStepLength());
	}

	@Override
	public synchronized double getSimTimeInSec() {
		return visTime;
	}

	public synchronized void setPotentialFieldContainer(final PotentialFieldContainer container) {
		this.potentialContainer = container;
	}

	public synchronized double getMaxSimTimeInSec() {
		return Step.toSimTimeInSec(getLastStep(), simTimeStepLength);
	}

	/**
	 * change to dataframe
	 *
	 * Returns all trajectories. E.g. also trajectories from pedestrian that are not already
	 * disappeared and pedestrians that are already reach their target
	 * 
	 * @return all trajectories
	 */
	public synchronized Table getAppearedPedestrians() {
		return trajectories.getAgents(trajectories.getMinStartTime(), getSimTimeInSec());
	}

	@Deprecated
	public synchronized Stream<Trajectory> getAppearedPedestriansAsTrajectories() {
		throw new UnsupportedOperationException("not jet implemented");
	}

	/**
	 * change to dataframe
	 *
	 * Returns all trajectories of pedestrians that are visible in the topography at the current
	 * time step e. g. they didn't reach their target and they already appear.
	 * 
	 * @return all trajectories of pedestrians that are visible at the current time step
	 */
	public synchronized Table getAlivePedestrians() {
		return trajectories.getAliveAgents(trajectories.getMinStartTime(), getSimTimeInSec());
	}

	@Deprecated
	public synchronized Stream<Trajectory> getAlivePedestriansAsTrajectories() {
		throw new UnsupportedOperationException("not jet implemented");
	}

	@Deprecated
	public synchronized Trajectory getTrajectory(int pedestrianId){
		throw new UnsupportedOperationException("not jet implemented.");
	}

	public boolean isEmpty() {
		return trajectories.isEmpty();
	}

	@Override
	public int getTopographyId() {
		return topographyId;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public boolean hasOutputChanged() {
		return outputChanged;
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


	@Override
	public void setAgentColoring(@NotNull AgentColoring agentColoring) {
		this.config.setAgentColoring(agentColoring);
	}

	@Override
	public boolean isAlive(int pedId) {
		return trajectories.getDeathTime(pedId) > getSimTimeInSec();
	}

	public synchronized void setAgentColoring(@NotNull final AttributesAgent attributesAgent) {
		this.attributesAgent = attributesAgent;
	}
}
