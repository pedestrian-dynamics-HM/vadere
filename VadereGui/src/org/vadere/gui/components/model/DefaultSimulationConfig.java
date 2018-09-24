package org.vadere.gui.components.model;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.vadere.gui.components.utils.ColorHelper;
import org.vadere.gui.components.utils.Resources;

public class DefaultSimulationConfig extends DefaultConfig {
	private static Resources resources = Resources.getInstance("global");
	private boolean showLogo = Boolean.valueOf(resources.getProperty("Logo.show"));
	private double densityScale = Double.valueOf(resources.getProperty("Density.measurementscale"));
	private double densityMeasurementRadius = Double.valueOf(resources.getProperty("Density.measurementradius"));
	private double densityStandardDerivation = Double.valueOf(resources.getProperty("Density.standardderivation"));
	private double pedestrianTorso = Double.valueOf(resources.getProperty("Pedestrian.Radius")) * 2;

	private boolean useRandomPedestrianColors = false;
	private boolean showPedestrianIds = false;
	private boolean showTargets = true;
	private boolean showSources = true;
	private boolean showObstacles = true;
	private boolean showStairs = true;
	private boolean showPedestrians = true;
	private boolean showWalkdirection = false;
	private boolean showTargetPotentialField = false;
	private boolean showPotentialField = false;
	private boolean showTrajectories = false;
	private boolean showGrid = false;
	private boolean showDensity = false;
	private boolean showGroups = false;
	protected final Color pedestrianDefaultColor = Color.BLUE;
	private Map<Integer, Color> pedestrianColors = new TreeMap<>();
	private Map<Integer, Color> randomColors = new HashMap<>();
	private double gridWidth = Double.valueOf(resources.getProperty("ProjectView.cellWidth"));
	private final double MIN_CELL_WIDTH = Double.valueOf(resources.getProperty("ProjectView.minCellWidth"));
	private final double MAX_CELL_WIDTH = Double.valueOf(resources.getProperty("ProjectView.maxCellWidth"));

	public DefaultSimulationConfig() {
		super();
	}

	public DefaultSimulationConfig(final DefaultSimulationConfig config) {
		super(config);

		this.randomColors = new HashMap<>();
		this.pedestrianColors = new HashMap<>();

		for (Map.Entry<Integer, Color> entry : config.pedestrianColors.entrySet()) {
			this.pedestrianColors.put(new Integer(entry.getKey()), new Color(entry.getValue().getRed(), entry
					.getValue().getGreen(), entry.getValue().getBlue()));
		}

		this.showPedestrianIds = config.showPedestrianIds;
		this.gridWidth = config.gridWidth;
		this.showDensity = config.showDensity;
		this.showTargetPotentialField = config.showTargetPotentialField;
		this.showWalkdirection = config.showWalkdirection;
		this.showGrid = config.showGrid;
		this.showPedestrians = config.showPedestrians;
		this.showLogo = config.showLogo;
		this.showStairs = config.showStairs;
		this.showGroups = config.showGroups;
		this.showPotentialField = config.showPotentialField;
	}

	public boolean isShowGroups() {
		return showGroups;
	}

	public void setShowGroups(boolean showGroups) {
		this.showGroups = showGroups;
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

	public boolean isShowTargets() {
		return showTargets;
	}

	public void setShowTargets(boolean showTargets) {
		this.showTargets = showTargets;
		setChanged();
	}

	public boolean isShowSources() {
		return showSources;
	}

	public void setShowSources(boolean showSources) {
		this.showSources = showSources;
		setChanged();
	}

	public boolean isShowObstacles() {
		return showObstacles;
	}

	public void setShowObstacles(boolean showObstacles) {
		this.showObstacles = showObstacles;
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

	public void clearRandomColors() {
		randomColors.clear();
	}

	public Color getRandomColor(int pedId) {
		if (!randomColors.containsKey(pedId)) {
			randomColors.put(pedId, ColorHelper.randomColor());
		}
		return randomColors.get(pedId);
	}

	public void setUseRandomPedestrianColors(final boolean useRandomPedestrianColors) {
		this.useRandomPedestrianColors = useRandomPedestrianColors;
	}

	public boolean isUseRandomPedestrianColors() {
		return useRandomPedestrianColors;
	}

	public void setGridWidth(double gridWidth) {
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

	public void setShowPedestrianIds(final boolean showPedestrianIds) {
		this.showPedestrianIds = showPedestrianIds;
	}

}
