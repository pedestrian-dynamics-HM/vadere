package org.vadere.gui.components.model;

import java.awt.*;

public class DefaultConfig {
	private Color obstacleColor = new Color(0.7f,0.7f,0.7f);
	private Color sourceColor = new Color(0.3333333333333333f, 0.6588235294117647f, 0.40784313725490196f);
	private Color targetColor = new Color(0.8666666666666667f, 0.51764705882352946f, 0.32156862745098042f);
	private Color targetChangerColor = new Color(1.00f, 0.60f, 0.00f);
	private Color absorbingAreaColor = new Color(0.76863f,0.30588f, 0.32157f);
	private Color densityColor = Color.RED;
	private Color stairColor = new Color(0.5058823529411764f, 0.4470588235294118f, 0.6980392156862745f);
	private Color pedestrianColor = new Color(0.2980392156862745f, 0.4470588235294118f, 0.7901960784313725f);
	private Color measurementAreaColor = Color.RED;
	private int measurementAreaAlpha = 140;
	private boolean changed = false;

	public DefaultConfig() {}

	public DefaultConfig(final DefaultConfig config) {
		this.sourceColor = config.sourceColor;
		this.targetColor = config.targetColor;
		this.targetChangerColor = config.targetChangerColor;
		this.absorbingAreaColor = config.absorbingAreaColor;
		this.densityColor = config.densityColor;
		this.obstacleColor = config.obstacleColor;
		this.measurementAreaColor = config.measurementAreaColor;
		this.measurementAreaAlpha = config.measurementAreaAlpha;
		this.stairColor = config.stairColor;
		this.changed = config.changed;
	}

	protected synchronized void setChanged() {
		this.changed = true;
	}

	public Color getObstacleColor() {
		return obstacleColor;
	}

	public void setObstacleColor(final Color obstacleColor) {
		this.obstacleColor = obstacleColor;
		setChanged();
	}

	public Color getStairColor() {
		return stairColor;
	}

	public void setStairColor(final Color stairColor) {
		this.stairColor = stairColor;
		setChanged();
	}

	public Color getMeasurementAreaColor() {
		return measurementAreaColor;
	}

	public void setMeasurementAreaColor(Color measurementAreaColor) {
		this.measurementAreaColor = measurementAreaColor;
	}

	public int getMeasurementAreaAlpha() {
		return measurementAreaAlpha;
	}

	public void setMeasurementAreaAlpha(int measurementAreaAlpha) {
		this.measurementAreaAlpha = measurementAreaAlpha;
	}

	public Color getSourceColor() {
		return sourceColor;
	}

	public void setSourceColor(Color sourceColor) {
		this.sourceColor = sourceColor;
		setChanged();
	}

	public Color getTargetColor() {
		return targetColor;
	}

	public void setTargetColor(final Color targetColor) {
		this.targetColor = targetColor;
		setChanged();
	}

	public Color getTargetChangerColor() {
		return targetChangerColor;
	}

	public void setTargetChangerColor(final Color targetChangerColor) {
		this.targetChangerColor = targetChangerColor;
		setChanged();
	}

	public Color getAbsorbingAreaColor() {
		return absorbingAreaColor;
	}

	public void setAbsorbingAreaColor(final Color absorbingAreaColor) {
		this.absorbingAreaColor = absorbingAreaColor;
		setChanged();
	}

	public void setDensityColor(final Color densityColor) {
		this.densityColor = densityColor;
		setChanged();
	}

	public Color getPedestrianColor() {
		return pedestrianColor;
	}

	public void setPedestrianColor(Color pedestrianColor) {
		this.pedestrianColor = pedestrianColor;
	}

	public Color getDensityColor() {
		return densityColor;
	}

	public synchronized boolean hasChanged() {
		return changed;
	}

	public synchronized void clearChange() {
		changed = false;
	}
}
