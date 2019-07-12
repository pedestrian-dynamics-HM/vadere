package org.vadere.gui.postvisualization.model;

import java.util.*;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.utils.Resources;

public class PostvisualizationConfig extends DefaultSimulationConfig {

	private static Resources resources = Resources.getInstance("postvisualization");

	private boolean recording = false;
	private boolean showAllTrajectories = true;
	private boolean showTrajecoriesOnSnapshot = false;
	private boolean showFaydedPedestrians = false;
	private boolean showAllTrajOnSnapshot = false;
	private boolean loadTopographyInformationsOnly = false;
	private boolean useEvacuationTimeColor = false;
	//private double gridWidth = Double.valueOf(resources.getProperty("PostVis.cellWidth"));
	private int fps = Integer.valueOf(resources.getProperty("PostVis.framesPerSecond"));

	private final int MAX_VELOCITY = Integer.valueOf(resources.getProperty("PostVis.maxFramePerSecond"));

	private Observable observable;

	public PostvisualizationConfig() {}

	public PostvisualizationConfig(final PostvisualizationConfig config) {
		super(config);
		this.fps = config.fps;
		//this.gridWidth = config.gridWidth;
		this.showAllTrajectories = config.showAllTrajectories;
		this.showFaydedPedestrians = config.showFaydedPedestrians;
		this.showTrajecoriesOnSnapshot = config.showTrajecoriesOnSnapshot;
		this.loadTopographyInformationsOnly = config.loadTopographyInformationsOnly;
		this.observable = config.observable;
		this.useEvacuationTimeColor = config.useEvacuationTimeColor;
	}

	public void setShowAllTrajectories(boolean showAllTrajectories) {
		this.showAllTrajectories = showAllTrajectories;
		setChanged();
	}

	public int getFps() {
		return fps;
	}

	public int getMaxVelocity() {
		return MAX_VELOCITY;
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

	public boolean isShowAllTrajectories() {
		return showAllTrajectories;
	}

	public boolean isShowTrajecoriesOnSnapshot() {
		return showTrajecoriesOnSnapshot;
	}

	public void setShowTrajecoriesOnSnapshot(final boolean showTrajecoriesOnSnapshot) {
		this.showTrajecoriesOnSnapshot = showTrajecoriesOnSnapshot;
	}

	public boolean isUseEvacuationTimeColor() {
		return useEvacuationTimeColor;
	}

	public boolean isShowFaydedPedestrians() {
		return showFaydedPedestrians;
	}

	public void setUseEvacuationTimeColor(boolean useEvacuationTimeColor) {
		this.useEvacuationTimeColor = useEvacuationTimeColor;
		setChanged();
	}

	public void setShowFaydedPedestrians(boolean showFaydedPedestrians) {
		this.showFaydedPedestrians = showFaydedPedestrians;
		setChanged();
	}

	public boolean isShowAllTrajOnSnapshot() {
		return showAllTrajOnSnapshot;
	}

	public void setShowAllTrajOnSnapshot(boolean showAllTrajOnSnapshot) {
		this.showAllTrajOnSnapshot = showAllTrajOnSnapshot;
		setChanged();
	}
}


