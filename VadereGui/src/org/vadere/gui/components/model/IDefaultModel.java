package org.vadere.gui.components.model;

import javax.swing.*;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.control.IScaleChangeListener;
import org.vadere.gui.components.control.IViewportChangeListener;
import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public interface IDefaultModel<T extends DefaultConfig> extends Iterable<ScenarioElement> {

	void addViewportChangeListener(final IViewportChangeListener listener);

	void removeViewportChangeListener(final IViewportChangeListener listener);

	void addScaleChangeListener(final IScaleChangeListener listener);

	void removeScaleChangeListener(final IScaleChangeListener listener);

	boolean setScale(final double scale);

	boolean setScaleWithoutChangingViewport(final double scale);

	void notifyScaleListeners();

	Color getScenarioElementColor(final ScenarioElementType elementType);

	/**
	 * zoom by a fixed factor into the topography by changing the viewport.
	 */
	boolean zoomIn();
	boolean zoomIn(double zoomFactor);

	/**
	 * zoom by a fixed factor out of the topography by changing the viewport.
	 */
	boolean zoomOut();
	boolean zoomOut(double zoomFactor);

	/**
	 * Set the current mouse position. This helps to draw the cursor.
	 * 
	 * @param mousePosition the current mouse position in window coordinates
	 */
	void setMousePosition(final Point mousePosition);

	/**
	 * Set the window position where the selection starts. This helps to calculate
	 * the selected area (start position, end position)
	 * 
	 * @param startSelectionPoint position where the selection starts in window coordinates
	 */
	void setStartSelectionPoint(final Point startSelectionPoint);

	/**
	 * Set a shape that framed the selected area. A renderer can draw this in
	 * a special way so that the user knows what is selected.
	 * 
	 * @param shape a shape that framed the selected area in world coordinates
	 */
	void setSelectionShape(final VShape shape);


	/**
	 * Sets the viewport bound, that is responsible for what the renderer will display.
	 * 
	 * @param viewportBound the area that will be displayed in world coordinates
	 */
	void setViewportBound(final Rectangle2D.Double viewportBound);

	/**
	 * resets the scenarioSize to the original scenario size.
	 */
	void resetTopographySize();

	/**
	 *
	 */
	void fireChangeViewportEvent(final Rectangle2D.Double viewportBound);

	/**
	 * To make the scrollPane available for manipulating its scrollbars in mousewheel-drag-mode in
	 * RectangleSelectionMode
	 */
	void addScrollPane(JScrollPane scrollPane);

	JScrollPane getScrollPane();

	/**
	 * Sets the window bound.
	 *
	 */
	void setWindowBound(final Rectangle2D.Double windowBound);

	/**
	 * Sets the voronoiDiagram that may be drawn.
	 * 
	 * @param voronoiDiagram the voronoi diagram
	 */
	void setVoronoiDiagram(final VoronoiDiagram voronoiDiagram);

	/**
	 * Tells the model that a element has been changed.
	 *
	 */
	void setElementHasChanged(final ScenarioElement element);

	VoronoiDiagram getVoronoiDiagram();

	/**
	 * Return the topography that is displayed.
	 * 
	 * @return the topography
	 */
	Topography getTopography();

	/**
	 * Return the current mouse position.
	 * 
	 * @return the current mouse position in world coordinates
	 */
	VPoint getMousePosition();

	/**
	 * Return the start selection position.
	 * 
	 * @return the start selection position in world coordinates
	 */
	VPoint getStartSelectionPoint();

	/**
	 * Returns the selection shape. A renderer can draw this in
	 * a special way so that the user knows what is selected.
	 * 
	 * @return the selection shape
	 */
	VShape getSelectionShape();

	/**
	 * Return the viewport bound in world coordinates.
	 * 
	 * @return the viewport bound in world coordinates
	 */
	Rectangle2D.Double getViewportBound();

	/**
	 * Returns the topography bound in world coordinates.
	 * 
	 * @return the topography bound in world coordinates
	 */
	Rectangle2D.Double getTopographyBound();


	/**
	 * Returns the window bound in window coordinates.
	 * 
	 * @return the window bound in window coordinates
	 */
	Rectangle2D.Double getWindowBound();

	/**
	 * Return the scale factor. A large scale factor results in
	 * displaying only a small part of the topography.
	 * 
	 * @return the scale factor
	 */
	double getScaleFactor();

	/**
	 * Return the grid resolution. The grid resolution is responsible for
	 * the refinement of the displayed grid. If the scale factor is large,
	 * the grid resolution shall be small.
	 * 
	 * @return the grid resolution
	 */
	double getGridResolution();

	/**
	 * Returns the width of the border of the topography.
	 * 
	 * @return the width of the border of the topography in world coordinates
	 */
	double getBoundingBoxWidth();

	/**
	 * Retrun true if the selection rectangle is visible, false otherwise
	 * 
	 * @return true if the selection rectangle is visible, false otherwise
	 */
	boolean isSelectionVisible();

	/**
	 * Return true if the user choose a measurement area for the voronoi diagram, otherwise false.
	 * 
	 * @return true if the user choose a measurement area for the voronoi diagram, otherwise false
	 */
	boolean isVoronoiDiagramAvailable();

	/**
	 * Return true if the voronoi diagram should be drawn.
	 * 
	 * @return true if the voronoi diagram should be drawn
	 */
	boolean isVoronoiDiagramVisible();

	/**
	 * hides the voronoi diagram so it will no longer be drawn and calculated.
	 */
	void hideVoronoiDiagram();

	/**
	 * makes the voronoi diagram visible, so if it is available it will be calculated and drawn.
	 */
	void showVoronoiDiagram();

	/**
	 * makes the selection rectangle visible
	 */
	void showSelection();

	/**
	 * hide the selection rectangle
	 */
	void hideSelection();

	/**
	 * Set the selection element. The element will be the element that contains the world position.
	 * If at the position is no element, the selection element will be null.
	 * 
	 * @param position the world position
	 * @return the selected element at the given world position or null if there is no such element
	 */
	ScenarioElement setSelectedElement(final VPoint position);

	/**
	 * Set the selection element.
	 * 
	 * @param selectedElement the new selected element
	 */
	void setSelectedElement(final ScenarioElement selectedElement);

	/*
	 * Deselect the currently selected element.
	 * Can be called even if no element is currently selected.
	 */
	void deselectSelectedElement();

	/**
	 * Returns the selected element, this may be null.
	 * 
	 * @return the selected element or null
	 */
	ScenarioElement getSelectedElement();

	/**
	 * Returns true if a element is selected.
	 * 
	 * @return true if an element is selected, otherwise false
	 */
	boolean isElementSelected();

	/**
	 * Add a SelectScenarioElementListener to the model. All listeners will be notified
	 * if the selection of an element change.
	 * 
	 * @param listener the listener that will be notified if the selection of an element change
	 */
	void addSelectScenarioElementListener(final ISelectScenarioElementListener listener);

	/**
	 * Remove a SelectScenarioElementListener from the model.
	 * 
	 * @param listener the listener that will be removed
	 */
	void removeSelectScenarioElementListener(final ISelectScenarioElementListener listener);

	void setMouseSelectionMode(final IMode selectionMode);

	IMode getMouseSelectionMode();

	/**
	 * Notify all observers of this model (not the SelectScenarioElementListener). Only the
	 * control should call this method.
	 */
	void notifyObservers();

	/**
	 * Notify all observers of this model (not the SelectScenarioElementListener). Only the
	 * control should call this method.
	 * 
	 * @param args some arguments
	 */
	void notifyObservers(final Object args);

	T getConfig();
}
