package org.vadere.gui.components.model;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.configuration2.Configuration;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.visualization.ColorHelper;

public class DefaultSimulationConfig extends DefaultConfig {

	private static final Configuration CONFIG = VadereConfig.getConfig();

	private boolean showLogo = CONFIG.getBoolean("SettingsDialog.showLogo");
	private double densityScale = CONFIG.getDouble("Density.measurementScale");
	private double densityMeasurementRadius = CONFIG.getDouble("Density.measurementRadius");
	private double densityStandardDerivation = CONFIG.getDouble("Density.standardDeviation");
	private double pedestrianTorso = CONFIG.getDouble("Pedestrian.radius") * 2;

	private boolean recording = false;
	private boolean interpolatePositions = true;
	private boolean showPedestrianIds = false;
	private boolean showPedestrianInOutGroup = false;
	private boolean showTargets = true;
	private boolean showTargetChangers = true;
	private boolean showAbsorbingAreas = true;
	private boolean showAerosolClouds = true;
	private boolean aerosolCloudsRecorded = false;
	private boolean showDroplets = true;
	private boolean showSources = true;
	private boolean showObstacles = true;
	private boolean showMeasurementArea = true;
	private boolean showStairs = true;
	private boolean showPedestrians = true;
	private boolean showContacts = true;
	private boolean contactsRecorded = false;
	private boolean showWalkdirection = false;
	private boolean showTargetPotentialField = false;
	private boolean showTargetPotentielFieldMesh = false;
	private boolean showPotentialField = false;
	private boolean showTrajectories = false;
	private boolean showGrid = false;
	private boolean showDensity = false;
	private boolean showGroups = false;
	protected final Color pedestrianDefaultColor = new Color(76, 114, 202);
	private Map<Integer, Color> pedestrianColors = new TreeMap<>();
	private Map<Integer, Color> randomColors = new HashMap<>();
	private Map<Integer, Color> selfCategoryColors = new HashMap<>();

	/*
	 * threshold above which pedestrian's color changes gradually depending on current degree of exposure
	 */
	private double lowerVisualizedExposure = 0;

	/*
	 * threshold below which pedestrian's color changes gradually depending on current degree of exposure
	 */
	private double upperVisualizedExposure = 1000;

	private Map<Integer, Color> informationStateColors = new HashMap<>();
	private double gridWidth = CONFIG.getDouble("ProjectView.cellWidth");
	private final double MIN_CELL_WIDTH = CONFIG.getDouble("ProjectView.minCellWidth");
	private final double MAX_CELL_WIDTH = CONFIG.getDouble("ProjectView.maxCellWidth");
	private AgentColoring agentColoring = AgentColoring.TARGET;

	public DefaultSimulationConfig() {
		super();
	}

	public DefaultSimulationConfig(final DefaultSimulationConfig config) {
		super(config);

		this.randomColors = new HashMap<>();
		this.pedestrianColors = new HashMap<>();
		this.selfCategoryColors = new HashMap<>();
		this.lowerVisualizedExposure = config.lowerVisualizedExposure;
		this.upperVisualizedExposure = config.upperVisualizedExposure;
		this.informationStateColors = new HashMap<>();

		for (Map.Entry<Integer, Color> entry : config.pedestrianColors.entrySet()) {
			this.pedestrianColors.put(new Integer(entry.getKey()), new Color(entry.getValue().getRed(), entry
					.getValue().getGreen(), entry.getValue().getBlue()));
		}

		this.showPedestrianIds = config.showPedestrianIds;
		this.showPedestrianInOutGroup = config.showPedestrianInOutGroup;
		this.gridWidth = config.gridWidth;
		this.showDensity = config.showDensity;
		this.showTargetPotentialField = config.showTargetPotentialField;
		this.showWalkdirection = config.showWalkdirection;
		this.showGrid = config.showGrid;
		this.showPedestrians = config.showPedestrians;
		this.showContacts = config.showContacts;
		this.showLogo = config.showLogo;
		this.showStairs = config.showStairs;
		this.showGroups = config.showGroups;
		this.showPotentialField = config.showPotentialField;
		this.showTargetPotentielFieldMesh = config.showTargetPotentielFieldMesh;
		this.agentColoring = config.agentColoring;
	}

	public boolean isShowGroups() {
		return showGroups;
	}

	public void setShowGroups(boolean showGroups) {
		this.showGroups = showGroups;
		setChanged();
	}

	public boolean isShowLogo() {
		return showLogo;
	}

	public void setShowLogo(boolean showLogo) {
		this.showLogo = showLogo;
		setChanged();
	}

	public boolean isShowPedestrians() {
		return showPedestrians;
	}

	public boolean isShowContacts() {
		return showContacts;
	}

	public boolean isContactsRecorded() {
		return contactsRecorded;
	}

	public void setContactsRecorded(boolean contactsRecorded) {
		this.contactsRecorded = contactsRecorded;
	}

	public boolean isShowWalkdirection() {
		return showWalkdirection;
	}

	public void setShowWalkdirection(boolean showWalkdirection) {
		this.showWalkdirection = showWalkdirection;
		setChanged();
	}

	public Color getPedestrianDefaultColor() {
		return pedestrianDefaultColor;
	}

	public void setShowPedestrians(boolean showPedestrians) {
		this.showPedestrians = showPedestrians;
		setChanged();
	}

	public void setShowContacts(boolean showContacts) {
		this.showContacts = showContacts;
		setChanged();
	}

	public boolean isShowTargets() {
		return showTargets;
	}

	public boolean isShowTargetChangers() { return showTargetChangers; }

	public boolean isShowAbsorbingAreas() {
		return showAbsorbingAreas;
	}

	public boolean isShowAerosolClouds() {
		return showAerosolClouds;
	}

	public boolean isAerosolCloudsRecorded() {
		return aerosolCloudsRecorded;
	}

	public void setAerosolCloudsRecorded(boolean aerosolCloudsRecorded) {
		this.aerosolCloudsRecorded = aerosolCloudsRecorded;
	}

	public boolean isShowDroplets() {
		return showDroplets;
	}

	public boolean isShowMeasurementAreas() {
		return showMeasurementArea;
	}

	public void setShowTargetPotentielFieldMesh(final boolean showTargetPotentielFieldMesh) {
		this.showTargetPotentielFieldMesh = showTargetPotentielFieldMesh;
		setChanged();
	}

	public boolean isShowTargetPotentielFieldMesh() {
		return showTargetPotentielFieldMesh;
	}

	public void setShowTargets(boolean showTargets) {
		this.showTargets = showTargets;
		setChanged();
	}

	public void setShowTargetChangers(boolean showTargetChangers) {
		this.showTargetChangers = showTargetChangers;
		setChanged();
	}

	public boolean isShowSources() {
		return showSources;
	}

	public void setShowSources(boolean showSources) {
		this.showSources = showSources;
		setChanged();
	}

	public void setShowAbsorbingAreas(boolean showAbsorbingAreas) {
		this.showAbsorbingAreas = showAbsorbingAreas;
		setChanged();
	}

	public void setShowAerosolClouds(boolean showAerosolClouds) {
		this.showAerosolClouds = showAerosolClouds;
		setChanged();
	}

	public void setShowDroplets(boolean showDroplets) {
		this.showDroplets = showDroplets;
		setChanged();
	}

	public boolean isShowObstacles() {
		return showObstacles;
	}

	public void setShowObstacles(boolean showObstacles) {
		this.showObstacles = showObstacles;
		setChanged();
	}

	public boolean isShowMeasurementArea(){
		return showMeasurementArea;
	}

	public void setShowMeasurementArea(boolean showMeasurementArea){
		this.showMeasurementArea = showMeasurementArea;
		setChanged();
	}

	public boolean isShowStairs() {
		return showStairs;
	}

	public void setShowStairs(boolean showStairs) {
		this.showStairs = showStairs;
		setChanged();
	}

	public boolean isShowTrajectories() {
		return showTrajectories;
	}

	public void setShowTrajectories(boolean showTrajectories) {
		this.showTrajectories = showTrajectories;
		setChanged();
	}

	public boolean isShowGrid() {
		return showGrid;
	}

	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
		setChanged();
	}

	public boolean isShowDensity() {
		return showDensity;
	}

	public void setShowDensity(boolean showDensity) {
		this.showDensity = showDensity;
		setChanged();
	}

	public void setShowTargetPotentialField(final boolean showTargetPotentialField) {
		this.showTargetPotentialField = showTargetPotentialField;
		setChanged();
	}

	public void setShowPotentialField(final boolean showPotentialField) {
		this.showPotentialField = showPotentialField;
		setChanged();
	}

	public double getDensityScale() {
		return densityScale;
	}

	public double getDensityMeasurementRadius() {
		return densityMeasurementRadius;
	}

	public double getDensityStandardDerivation() {
		return densityStandardDerivation;
	}

	public double getPedestrianTorso() {
		return pedestrianTorso;
	}

	public boolean isShowTargetPotentialField() {
		return showTargetPotentialField;
	}

	public boolean isShowPotentialField() {
		return showPotentialField;
	}

	public Optional<Color> getColorByTargetId(final int targetId) {
		return Optional.ofNullable(pedestrianColors.get(targetId));
	}

	public void setPedestrianColor(final int targetId, final Color color) {
		this.pedestrianColors.put(targetId, color);
		setChanged();
	}

	public void addPedestrianColors(final Map<Integer, Color> pedestrianColors, final boolean override) {
		if (override) {
			this.pedestrianColors.putAll(pedestrianColors);
		} else {
			for (Map.Entry<Integer, Color> entry : pedestrianColors.entrySet()) {
				if (!this.pedestrianColors.containsKey(entry.getKey())) {
					this.pedestrianColors.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	public void setAgentColoring(final AgentColoring agentColoring) {
		if(agentColoring != this.agentColoring) {
			this.agentColoring = agentColoring;
			setChanged();
		}
	}

	public AgentColoring getAgentColoring() {
		return agentColoring;
	}

	public void clearRandomColors() {
		randomColors.clear();
	}

	public Color getRandomColor(int pedId) {
		if (!randomColors.containsKey(pedId)) {
			randomColors.put(pedId, ColorHelper.randomColor());
		}
		return randomColors.get(pedId);
	}

	public void setSelfCategoryColor(SelfCategory selfCategory, final Color color) {
		this.selfCategoryColors.put(selfCategory.ordinal(), color);
		setChanged();
	}

	public void setInformationStateColor(InformationState informationState, final Color color) {
		this.informationStateColors.put(informationState.ordinal(), color);
		setChanged();
	}

	public Color getSelfCategoryColor(SelfCategory selfCategory) {
		Color color = getPedestrianDefaultColor();

		if (selfCategoryColors.containsKey(selfCategory.ordinal())) {
			color = selfCategoryColors.get(selfCategory.ordinal());
		}

		return color;
	}

	public double getLowerVisualizedExposure() {
		return lowerVisualizedExposure;
	}

	public void setLowerVisualizedExposure(double lowerVisualizedExposure) {
		this.lowerVisualizedExposure = lowerVisualizedExposure;
	}

	public void setUpperVisualizedExposure(double upperVisualizedExposure) {
		this.upperVisualizedExposure = upperVisualizedExposure;
	}

	public double getUpperVisualizedExposure() {
		return this.upperVisualizedExposure;
	}

	public Color getHealthStatusColor(Boolean isInfectious, double degreeOfExposure) {
		Color color;

		if (isInfectious) {
			color = getInfectiousColor();
		} else {
			color = getInterpolatedExposureColor(degreeOfExposure);
		}
		return color;
	}

	/*
	 * defines a truncated color transition depending on an agent's degree of exposure.
	 */
	private Color getInterpolatedExposureColor(double degreeOfExposure) {
		Color color;
		float t = (float) ((degreeOfExposure - lowerVisualizedExposure) / upperVisualizedExposure);

		Color susceptibleColor = getPedestrianDefaultColor();
		Color exposedColor = getExposedColor();

		if (degreeOfExposure <= lowerVisualizedExposure) {
			color = susceptibleColor;
		} else if (degreeOfExposure >= upperVisualizedExposure) {
			color = exposedColor;
		} else {
			color = ColorHelper.improvedColorInterpolation(susceptibleColor, exposedColor, t);
			// alternatively use:
			// color = ColorHelper.standardColorInterpolation(susceptibleColor, exposedColor, t);
		}
		return color;
	}

	public Color getInformationStateColor(InformationState informationState) {
		Color color = getPedestrianDefaultColor();

		if (informationStateColors.containsKey(informationState.ordinal())) {
			color = informationStateColors.get(informationState.ordinal());
		}

		return color;
	}

	public void setGridWidth(final double gridWidth) {
		this.gridWidth = gridWidth;
	}

	public double getGridWidth() {
		return gridWidth;
	}

	public double getMaxCellWidth() {
		return MAX_CELL_WIDTH;
	}

	public double getMinCellWidth() {
		return MIN_CELL_WIDTH;
	}

	public boolean isShowPedestrianIds() {
		return showPedestrianIds;
	}

	public boolean isShowPedestrianInOutGroup() { return showPedestrianInOutGroup; }

	public void setShowPedestrianIds(final boolean showPedestrianIds) {
		this.showPedestrianIds = showPedestrianIds;
	}

	public void setShowPedestrianInOutGroup(final boolean showPedestrianInOutGroup) {
		this.showPedestrianInOutGroup = showPedestrianInOutGroup;
	}

	public boolean isShowFaydedPedestrians() {
		return false;
	}

	public boolean isInterpolatePositions() {
		return interpolatePositions;
	}

	public void setInterpolatePositions(final boolean interpolatePositions) {
		this.interpolatePositions = interpolatePositions;
		setChanged();
	}

	public void setRecording(boolean recording) {
		this.recording = recording;
	}

	public boolean isRecording() {
		return recording;
	}
}
