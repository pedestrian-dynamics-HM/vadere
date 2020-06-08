package org.vadere.gui.postvisualization.model;

import org.apache.commons.configuration2.Configuration;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.util.config.VadereConfig;

import java.util.Observable;

public class PostvisualizationConfig extends DefaultSimulationConfig {

	private static final Configuration CONFIG = VadereConfig.getConfig();

	private boolean showAllTrajectories = true;
	private boolean showFaydedPedestrians = false;
	private boolean loadTopographyInformationsOnly = false;

	private int fps = CONFIG.getInt("PostVis.framesPerSecond");
	private final int MAX_VELOCITY = CONFIG.getInt("PostVis.maxFramePerSecond");
	private double timeResolution = CONFIG.getDouble("PostVis.timeResolution");
	private Observable observable;

	public PostvisualizationConfig() {}

	public PostvisualizationConfig(final PostvisualizationConfig config) {
		super(config);
		this.fps = config.fps;
		//this.gridWidth = config.gridWidth;
		this.showAllTrajectories = config.showAllTrajectories;
		this.showFaydedPedestrians = config.showFaydedPedestrians;
		this.loadTopographyInformationsOnly = config.loadTopographyInformationsOnly;
		this.observable = config.observable;
	}

	public void setShowAllTrajectories(boolean showAllTrajectories) {
		this.showAllTrajectories = showAllTrajectories;
		setChanged();
	}

	public void setTimeResolution(double timeResolution) {
		this.timeResolution = timeResolution;
		CONFIG.setProperty("PostVis.timeResolution", timeResolution);
		setChanged();
	}

	public int getFps() {
		return fps;
	}

	public double getTimeResolution() {
		return timeResolution;
	}

	public int getMaxVelocity() {
		return MAX_VELOCITY;
	}

	public void setFps(final int fps) {
		this.fps = fps;
		CONFIG.setProperty("PostVis.framesPerSecond", fps);
		setChanged();
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

	public boolean isShowFaydedPedestrians() {
		return showFaydedPedestrians;
	}

	public void setShowFaydedPedestrians(boolean showFaydedPedestrians) {
		this.showFaydedPedestrians = showFaydedPedestrians;
		setChanged();
	}
}


