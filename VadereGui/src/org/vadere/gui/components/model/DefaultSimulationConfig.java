package org.vadere.gui.components.model;

import java.awt.*;

import org.vadere.gui.components.utils.Resources;

public class DefaultSimulationConfig extends DefaultConfig {
	private static Resources resources = Resources.getInstance("global");
	private boolean showLogo = Boolean.valueOf(resources.getProperty("Logo.show"));
	private double densityScale = Double.valueOf(resources.getProperty("Density.measurementscale"));
	private double densityMeasurementRadius = Double.valueOf(resources.getProperty("Density.measurementradius"));
	private double densityStandardDerivation = Double.valueOf(resources.getProperty("Density.standardderivation"));
	private double pedestrianTorso = Double.valueOf(resources.getProperty("Pedestrian.Radius")) * 2;

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

	public DefaultSimulationConfig() {
		super();
	}

	public DefaultSimulationConfig(final DefaultSimulationConfig config) {
		super(config);
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
}
