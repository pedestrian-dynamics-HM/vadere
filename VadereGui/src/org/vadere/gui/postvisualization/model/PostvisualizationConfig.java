package org.vadere.gui.postvisualization.model;

import java.awt.*;
import java.util.*;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.utils.Resources;

public class PostvisualizationConfig extends DefaultSimulationConfig {

	private static Resources resources = Resources.getInstance("postvisualization");

	private boolean recording = false;
	private boolean showAllTrajectories = true;
	private boolean showTrajecoriesOnSnapshot = false;
	private boolean showFaydedPedestrians = false;
	private boolean showPedestrianIds = false;
	private boolean loadTopographyInformationsOnly = false;
	private double gridWidth = Double.valueOf(resources.getProperty("PostVis.cellWidth"));
	private int fps = Integer.valueOf(resources.getProperty("PostVis.framesPerSecond"));

	private final int MAX_VELOCITY = Integer.valueOf(resources.getProperty("PostVis.maxFramePerSecond"));
	private final double MIN_CELL_WIDTH = Double.valueOf(resources.getProperty("PostVis.minCellWidth"));
	private final double MAX_CELL_WIDTH = Double.valueOf(resources.getProperty("PostVis.maxCellWidth"));

	private Map<Integer, Color> pedestrianColors = new TreeMap<>();

	private Observable observable;

	public PostvisualizationConfig() {}

	public PostvisualizationConfig(final PostvisualizationConfig config) {
		super(config);
		this.fps = config.fps;
		this.gridWidth = config.gridWidth;

		this.pedestrianColors = new HashMap<>();

		for (Map.Entry<Integer, Color> entry : config.pedestrianColors.entrySet()) {
			this.pedestrianColors.put(new Integer(entry.getKey()), new Color(entry.getValue().getRed(), entry
					.getValue().getGreen(), entry.getValue().getBlue()));
		}

		this.showAllTrajectories = config.showAllTrajectories;
		this.showFaydedPedestrians = config.showFaydedPedestrians;
		this.showTrajecoriesOnSnapshot = config.showTrajecoriesOnSnapshot;
		this.loadTopographyInformationsOnly = config.loadTopographyInformationsOnly;
		this.showPedestrianIds = config.showPedestrianIds;
		this.observable = config.observable;
	}

	public boolean isShowAllTrajectories() {
		return showAllTrajectories;
	}

	public void setShowAllTrajectories(boolean showAllTrajectories) {
		this.showAllTrajectories = showAllTrajectories;
		setChanged();
	}

	public void setGridWidth(double gridWidth) {
		this.gridWidth = gridWidth;
	}

	public double getGridWidth() {
		return gridWidth;
	}

	public int getFps() {
		return fps;
	}

	public Optional<Color> getColorByTargetId(final int targetId) {
		return Optional.ofNullable(pedestrianColors.get(targetId));
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

	public void setPedestrianColor(final int targetId, final Color color) {
		this.pedestrianColors.put(targetId, color);
		setChanged();
	}

	public boolean isShowTrajecoriesOnSnapshot() {
		return showTrajecoriesOnSnapshot;
	}

	public void setShowTrajecoriesOnSnapshot(boolean showTrajecoriesOnSnapshot) {
		this.showTrajecoriesOnSnapshot = showTrajecoriesOnSnapshot;
		setChanged();
	}

	public boolean isShowFaydedPedestrians() {
		return showFaydedPedestrians;
	}

	public void setShowFaydedPedestrians(boolean showFaydedPedestrians) {
		this.showFaydedPedestrians = showFaydedPedestrians;
		setChanged();
	}

	public int getMaxVelocity() {
		return MAX_VELOCITY;
	}

	public double getMaxCellWidth() {
		return MAX_CELL_WIDTH;
	}

	public double getMinCellWidth() {
		return MIN_CELL_WIDTH;
	}

	public void setFps(final int fps) {
		this.fps = fps;
	}

	public void setRecording(boolean recording) {
		this.recording = recording;
	}

	public boolean isRecording() {
		return recording;
	}

	public boolean isLoadTopographyInformationsOnly() {
		return loadTopographyInformationsOnly;
	}

	public void setLoadTopographyInformationsOnly(final boolean loadTopographyInformationsOnly) {
		this.loadTopographyInformationsOnly = loadTopographyInformationsOnly;
	}

	public boolean isShowPedestrianIds() {
		return showPedestrianIds;
	}

	public void setShowPedestrianIds(final boolean showPedestrianIds) {
		this.showPedestrianIds = showPedestrianIds;
	}
}


