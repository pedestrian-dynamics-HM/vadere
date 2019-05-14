package org.vadere.gui.topographycreator.model;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.model.DefaultConfig;
import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Teleporter;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Observer;
import java.util.function.Predicate;

public interface IDrawPanelModel<T extends DefaultConfig> extends IDefaultModel<T>, Iterable<ScenarioElement> {

	@Override
	void notifyObservers();

	/**
	 * Build a new Topography out of the current state of the DrawPanelModel by using the
	 * TopographyBuilder.
	 * 
	 * @return a complete new TopographyElement
	 */
	Topography build();

	/**
	 * Part of the observer-pattern. Adds an observer that will be notified about the changes of
	 * this panelModel.
	 *
	 * @param observer the observer that will be notified about the change of this panelModel.
	 */
	void addObserver(Observer observer);

	/**
	 * Changes the topography bound (cutting).
	 * 
	 * @param scenarioBound the new topography bound
	 */
	void setTopographyBound(final VRectangle scenarioBound);

	/**
	 * Returns the used font for displaying informations and so on.
	 * 
	 * @return the used font
	 */
	Font getFont();

	/**
	 * Scales the whole topography, so every topography element will be scaled and will be
	 * translated to the correct position. Pedestrians has only to be translated.
	 * 
	 * @param scale the scale factor has to be greater than zero
	 */
	void scaleTopography(final double scale);


	/**
	 * True if the user is selecting a topography element, otherwise false.
	 * 
	 * @return true if the user is selecting a topography element, otherwise false.
	 */
	@Override
	boolean isSelectionVisible();


	/**
	 * After this call the selction shape will be painted.
	 */
	@Override
	void showSelection();

	/**
	 * After this call the selction shape will no longer be painted.
	 */
	@Override
	void hideSelection();


	/**
	 * cleans the whole topography, after this call there is no topography element in the topography
	 * and
	 * resetTopographySize() will be called.
	 */
	void resetScenario();


	@Override
	ScenarioElement getSelectedElement();

	Color getCursorColor();

	void setCursorColor(Color red);

	@Override
	void setMouseSelectionMode(IMode selectionMode);

	@Override
	IMode getMouseSelectionMode();

	Cursor getCursor();

	void setCursor(Cursor cursor);

	double getScalingFactor();

	void setScalingFactor(double scalingFactor);

	void setVadereScenario(Scenario vadereScenario);

	Teleporter getTeleporter();

	void setTeleporter(Teleporter teleporter);

	// double getFinishTime();

	void addShape(ScenarioElement shape);

	ScenarioElement removeElement(VPoint position);

	/**
	 * @Null
	 * @param position
	 * @return
	 */
	@Override
	ScenarioElement setSelectedElement(VPoint position);

	boolean removeElement(ScenarioElement element);

	ScenarioElement deleteLastShape(ScenarioElementType type);

	ScenarioElement deleteLastShape();

	void switchType(ScenarioElementType type);

	ScenarioElementType getCurrentType();

	Scenario getScenario();

	void setTopography(Topography topography);

	@Override
	void notifyObservers(final Object string);

	int getBoundId();

	@Override
	void setSelectedElement(ScenarioElement selectedElement);

	VShape translate(Point vector);

	VShape resize(Point start, Point end);

	boolean isPrototypeVisble();

	VShape getPrototypeShape();

	void setPrototypeShape(VShape prototypeShape);

	void hidePrototypeShape();

	void showPrototypeShape();

	ScenarioElement getCopiedElement();

	void setCopiedElement(ScenarioElement copiedElement);

	VShape translate(VPoint vector);

	VShape translateElement(ScenarioElement elementToCopy, VPoint diff);

	void removeObstacleIf(final @NotNull Predicate<Obstacle> predicate);

	void removeMeasurementAreaIf(final @NotNull Predicate<MeasurementArea> predicate);

	List<Obstacle> getObstacles();

	List<MeasurementArea> getMeasurementAreas();

	Rectangle2D.Double getBounds();

	default VPoint translateVectorCoordinates(Point point) {
		return new VPoint(point.x / getScaleFactor(), getTopography().getBounds().height - point.y / getScaleFactor());
	}

}
