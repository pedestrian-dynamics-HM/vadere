package org.vadere.gui.components.model;

import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.control.IScaleChangeListener;
import org.vadere.gui.components.control.IViewportChangeListener;
import org.vadere.gui.components.control.ViewportChangeEvent;
import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.swing.*;

public abstract class DefaultModel<T extends DefaultConfig> extends Observable implements IDefaultModel<T> {
	// private static final int BORDER_WIDTH = 20;
	// private static final int BORDER_HEIGHT = 20;

	private static Logger log = Logger.getLogger(DefaultModel.class);

	private IMode mouseSelectionMode;

	protected ScenarioElement selectedElement;

	protected static final double MAX_SCALE_FACTOR = 1000;

	protected static final double MIN_SCALE_FACTOR = 0.01;

	protected double scaleFactor;

	private boolean showSelection;

	private boolean showVoroniDiagram;

	private VPoint cursorWorldPosition;

	private VPoint startSelectionPoint;

	protected Rectangle2D.Double viewportBound;

	private Rectangle2D.Double windowBound;

	private VShape selectionShape;

	private VoronoiDiagram voronoiDiagram;

	private List<ISelectScenarioElementListener> selectScenarioElementListener;

	private final List<IViewportChangeListener> viewportChangeListeners;

	private final List<IScaleChangeListener> scaleChangeListeners;

	private JScrollPane scrollPane;

	public T config;

	public DefaultModel(final T config) {
		this.config = config;
		this.scaleFactor = 50;
		this.windowBound = new Rectangle2D.Double(0, 0, 50, 50);
		this.viewportBound = new Rectangle2D.Double(0, 0, 50, 50);
		this.cursorWorldPosition = VPoint.ZERO;
		this.selectScenarioElementListener = new LinkedList<>();
		this.voronoiDiagram = null;
		this.showVoroniDiagram = true;
		this.showSelection = false;
		this.mouseSelectionMode = new DefaultSelectionMode(this);
		this.viewportChangeListeners = new ArrayList<>();
		this.scaleChangeListeners = new ArrayList<>();
	}

	@Override
	public Color getScenarioElementColor(final ScenarioElementType elementType) {
		Color c;
		switch (elementType) {
			case OBSTACLE:
				c = getConfig().getObstacleColor();
				break;
			case PEDESTRIAN:
				c = getConfig().getPedestrianColor();
				break;
			case SOURCE:
				c = getConfig().getSourceColor();
				break;
			case STAIRS:
				c = getConfig().getStairColor();
				break;
			case TARGET:
				c = getConfig().getTargetColor();
				break;
            case TARGET_CHANGER:
                c = getConfig().getTargetChangerColor();
                break;
			case ABSORBING_AREA:
				c = getConfig().getAbsorbingAreaColor();
				break;
			case AEROSOL_CLOUD:
				c = getConfig().getAerosolCloudColor();
				break;
			case DROPLETS:
				c = getConfig().getDropletsColor();
				break;
			default:
				c = Color.RED;
		}
		return c;
	}

	@Override
	public void addScrollPane(JScrollPane scrollPane) {
		this.scrollPane = scrollPane;
	}

	@Override
	public JScrollPane getScrollPane() {
		return scrollPane;
	}

	@Override
	public boolean zoomIn() {
		double scale = scaleFactor * 1.2;
		return setScale(scale);
	}

	@Override
	public boolean zoomIn(double zoomFactor) {
		double scale = scaleFactor * zoomFactor;
		return setScale(scale);
	}

	@Override
	public boolean zoomOut() {
		double scale = scaleFactor / 1.2;
		return setScale(scale);
	}

	@Override
	public boolean zoomOut(double zoomFactor) {
		double scale = scaleFactor / zoomFactor;
		return setScale(scale);
	}

	@Override
	public boolean setScale(final double scale) {
		double oldScale = scaleFactor;
		boolean hasChanged = setScaleWithoutChangingViewport(scale);

		// update the viewport, since it depends on the scaleFactor
		if (hasChanged) {
			Rectangle2D.Double oldViewPort = getViewportBound();
			Rectangle2D.Double newViewPort = new Rectangle2D.Double(
					oldViewPort.getMinX(),
					oldViewPort.getMinY(),
					oldViewPort.getWidth() * oldScale / scaleFactor,
					oldViewPort.getHeight() * oldScale / scaleFactor);
			setViewportBound(newViewPort);
			setChanged();
		}
		return hasChanged;
	}

	@Override
	public boolean setScaleWithoutChangingViewport(double scale) {
		boolean hasChanged = true;

		if (scale < MIN_SCALE_FACTOR) {
			this.scaleFactor = MIN_SCALE_FACTOR;
		} else if (scale > MAX_SCALE_FACTOR) {
			this.scaleFactor = MAX_SCALE_FACTOR;
		} else if (scale != this.scaleFactor) {
			this.scaleFactor = scale;
		} else {
			hasChanged = false;
		}

		if(hasChanged) {
			setChanged();
		}

		return hasChanged;
	}

	protected synchronized void notifyViewportListeners(final ViewportChangeEvent event) {
		for (IViewportChangeListener listener : viewportChangeListeners) {
			listener.viewportChange(event);
		}
	}

	@Override
	public synchronized void notifyScaleListeners() {
		for (IScaleChangeListener listener : scaleChangeListeners) {
			listener.scaleChange(getScaleFactor());
		}
	}

	@Override
	public synchronized void addViewportChangeListener(final IViewportChangeListener listener) {
		viewportChangeListeners.add(listener);
	}

	@Override
	public synchronized void removeViewportChangeListener(final IViewportChangeListener listener) {
		viewportChangeListeners.remove(listener);
	}

	@Override
	public synchronized void addScaleChangeListener(final IScaleChangeListener listener) {
		this.scaleChangeListeners.add(listener);
	}

	@Override
	public synchronized void removeScaleChangeListener(final IScaleChangeListener listener) {
		this.scaleChangeListeners.remove(listener);
	}

	/*
	 * @Override
	 * public boolean zoomIn() {
	 * if (scaleFactor < MAX_SCALE_FACTOR) {
	 * double w = getViewportBound().width / 1.2;
	 * double h = getViewportBound().height / 1.2;
	 * double x = Math.max(0, cursorWorldPosition.x - w / 2);
	 * double y = Math.max(0, cursorWorldPosition.y - h / 2);
	 * setViewportBound(new VRectangle(x, y, w, h));
	 * return true;
	 * }
	 * return false;
	 * }
	 * 
	 * @Override
	 * public boolean zoomOut() {
	 * double w = Math.min(getTopographyBound().getWidth(), viewportBound.width * 1.2);
	 * double h = Math.min(getTopographyBound().getWidth(), viewportBound.height * 1.2);
	 * 
	 * double x = Math.max(0, Math.min(cursorWorldPosition.x - w / 2,
	 * getTopographyBound().getWidth() - w));
	 * double y = Math.max(0, Math.min(cursorWorldPosition.y - h / 2,
	 * getTopographyBound().getHeight() - h));
	 * setViewportBound(new VRectangle(x, y, w, h));
	 * return true;
	 * }
	 */

	@Override
	public synchronized void setMousePosition(final Point mousePosition) {
		// this is needed cause of the mirrowing!
		VPoint mouseWorldPosition = pixelToWorld(new VPoint(mousePosition.x, mousePosition.y));
		double factor = Math.max(10, 1 / getGridResolution());
		cursorWorldPosition = new VPoint((Math.round(mouseWorldPosition.x * factor)) / factor,
				(Math.round(mouseWorldPosition.y * factor)) / factor);
		setChanged();
	}

	@Override
	public synchronized void setStartSelectionPoint(final Point startSelectionPoint) {
		VPoint worldPosition = pixelToWorld(new VPoint(startSelectionPoint.x, startSelectionPoint.y));
		double factor = Math.max(10, 1 / getGridResolution());
		this.startSelectionPoint = new VPoint((Math.round(worldPosition.x * factor)) / factor,
				(Math.round(worldPosition.y * factor)) / factor);
		setChanged();
	}

	@Override
	public synchronized void setSelectionShape(final VShape shape) {
		selectionShape = shape;
		setChanged();
	}

	@Override
	public synchronized void fireChangeViewportEvent(final Rectangle2D.Double viewportBound) {
		notifyViewportListeners(new ViewportChangeEvent(viewportBound));
	}

	@Override
	public synchronized void setViewportBound(final Rectangle2D.Double viewportBound) {
		Rectangle2D.Double oldViewportBound = this.viewportBound;

		if (!oldViewportBound.equals(viewportBound)) {
			this.viewportBound = viewportBound;
			setChanged();
		}
	}

	@Override
	public VPoint getMousePosition() {
		return cursorWorldPosition;
	}

	@Override
	public VPoint getStartSelectionPoint() {
		return startSelectionPoint;
	}

	@Override
	public VShape getSelectionShape() {
		return selectionShape;
	}

	@Override
	public Rectangle2D.Double getViewportBound() {
		return viewportBound;
	}

	/*
	 * @Override
	 * public Rectangle2D.Double getTopographyBound() {
	 * return getTopography().getBounds();
	 * }
	 */

	@Override
	public Rectangle2D.Double getTopographyBound() {
		Rectangle2D.Double topographyBound = null;
		if (getTopography() != null) {
			topographyBound = getTopography().getBounds();
		}

		return topographyBound;
	}

	@Override
	public Rectangle2D.Double getWindowBound() {
		return windowBound;
	}

	@Override
	public double getScaleFactor() {
		return scaleFactor;
	}

	@Override
	public double getGridResolution() {
		return getOptimalGridResolution(viewportBound);
	}

	@Override
	public double getBoundingBoxWidth() {
		return getTopography().getBoundingBoxWidth();
	}

	@Override
	public boolean isVoronoiDiagramAvailable() {
		return voronoiDiagram != null;
	}

	@Override
	public boolean isVoronoiDiagramVisible() {
		return showVoroniDiagram;
	}

	@Override
	public synchronized void setVoronoiDiagram(final VoronoiDiagram voronoiDiagram) {
		this.voronoiDiagram = voronoiDiagram;
	}

	@Override
	public VoronoiDiagram getVoronoiDiagram() {
		return voronoiDiagram;
	}

	@Override
	public boolean isSelectionVisible() {
		return showSelection;
	}

	@Override
	public void hideVoronoiDiagram() {
		showVoroniDiagram = false;
		setChanged();
	}

	@Override
	public void showVoronoiDiagram() {
		showVoroniDiagram = true;
		setChanged();
	}

	@Override
	public void showSelection() {
		showSelection = true;
		setChanged();
	}

	@Override
	public void hideSelection() {
		showSelection = false;
		setChanged();
	}

	@Override
	public synchronized void setWindowBound(final Rectangle2D.Double windowBound) {
		this.windowBound = windowBound;
		setChanged();
	}

	@Override
	public synchronized ScenarioElement setSelectedElement(final VPoint position) {
		getElementsByPosition(position).ifPresent(this::setSelectedElement);
		return selectedElement;
	}

	private synchronized Optional<ScenarioElement> getElementsByPosition(final VPoint position) {
		return getElements(e -> e.getShape().intersects(new Rectangle2D.Double(position.x - 0.1, position.y - 0.1, 0.2, 0.2))).findFirst();
	}

	protected synchronized ScenarioElement getClickedElement(final VPoint position) {
		Optional<ScenarioElement> optional = getElementsByPosition(position);
		if (optional.isPresent())
			return optional.get();
		return null;
	}

	protected synchronized Stream<ScenarioElement> getElements(final Predicate<ScenarioElement> predicate) {
		return StreamSupport.stream(this.spliterator(), false).filter(predicate);
	}

	@Override
	public synchronized void setSelectedElement(final ScenarioElement selectedElement) {
		this.selectedElement = selectedElement;
		notifySelectSecenarioElementListener(selectedElement);
	}

	@Override
	public synchronized void deselectSelectedElement() {
		setSelectedElement((ScenarioElement) null);
	}

	@Override
	public boolean isElementSelected() {
		return selectedElement != null;
	}

	@Override
	public ScenarioElement getSelectedElement() {
		return selectedElement;
	}

	@Override
	public synchronized void addSelectScenarioElementListener(final ISelectScenarioElementListener listener) {
		this.selectScenarioElementListener.add(listener);
	}

	@Override
	public synchronized void removeSelectScenarioElementListener(final ISelectScenarioElementListener listener) {
		this.selectScenarioElementListener.remove(listener);
	}

	@Override
	public void setMouseSelectionMode(final IMode selectionMode) {
		mouseSelectionMode = selectionMode;
	}

	@Override
	public IMode getMouseSelectionMode() {
		return mouseSelectionMode;
	}

	@Override
	public void setElementHasChanged(final ScenarioElement element) {
		setChanged();
	}

	protected synchronized void notifySelectSecenarioElementListener(final ScenarioElement scenarioElement) {
		for (ISelectScenarioElementListener listener : selectScenarioElementListener) {
			listener.selectionChange(scenarioElement);
		}
	}

	protected synchronized void calculateScaleFactor() {
		scaleFactor = Math.min(getWindowBound().getWidth() / getViewportBound().getWidth(),
				getWindowBound().getHeight() / getViewportBound().getHeight());
	}

	/**
	 *
	 * @param pInPixel the mouse position of the mouse event
	 * @return
	 */
	protected synchronized VPoint pixelToWorld(final VPoint pInPixel) {
		if(pInPixel != null && getTopographyBound() != null) {
			return new VPoint(pInPixel.getX() / scaleFactor + getTopographyBound().getMinX(),
					getTopographyBound().getMinY() + (getTopographyBound().getHeight() * scaleFactor - pInPixel.getY()) / scaleFactor);
		}
		return VPoint.ZERO;
		/*
		 * return new VPoint(pInPixel.getX() / scaleFactor + getTopographyBound().getX() +
		 * getViewportBound().getX(),
		 * (getWindowBound().getHeight()-pInPixel.getY()) / scaleFactor + viewportBound.getY() +
		 * getTopographyBound().getY());
		 */
	}

	/**
	 * Computes an optimal grid resolution for the given floor. The purpose of
	 * the grid is to allow the estimation of distances without covering too
	 * much of the drawn observation area. The resolution is based on the
	 * width/height of the floor.
	 */
	private static double getOptimalGridResolution(final Rectangle2D bounds) {
		double boundsMax = Math.max(bounds.getWidth(), bounds.getHeight());
		double gridResolution = 100.0;

		if (boundsMax <= 500.0) {
			if (boundsMax <= 50.0) {
				if (boundsMax <= 5.0) {
					if (boundsMax <= 0.5) {
						gridResolution = 0.01;
					} else {
						gridResolution = 0.1;
					}
				} else {
					gridResolution = 1.0;
				}
			} else {
				gridResolution = 10.0;
			}
		}

		return gridResolution;
	}

	@Override
	public T getConfig() {
		return config;
	}

	/*public void startTriangulation() {
		if(!triangulationTriggered) {
			triangulationTriggered = true;
			VRectangle bound = new VRectangle(getTopographyBound());
			Collection<Obstacle> obstacles = Topography.createObstacleBoundary(getTopography());
			obstacles.addAll(getTopography().getObstacles());

			List<VShape> shapes = obstacles.stream().map(obstacle -> obstacle.getShape()).collect(Collectors.toList());

			IDistanceFunction distanceFunc = new DistanceFunction(bound, shapes);
			PSDistmesh meshImprover = new PSDistmesh(
					distanceFunc,
					p -> Math.min(1.0 + Math.pow(Math.max(-distanceFunc.apply(p), 0), 2), 4.0),
					0.3,
					bound, getTopography().getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()));


			triangles = meshImprover.getTriangles();
			//	meshImprover.improve();
			Thread t = new Thread(() -> {
				while(!meshImprover.isFinished()) {
					meshImprover.improve();
					setChanged();
					notifyObservers();
				}
				Function<VTriangle, Color> colorFunction = f -> {
					float grayScale = (float) meshImprover.getQuality(f);
					return new Color(grayScale, grayScale, grayScale);
				};

				log.info(TexGraphGenerator.toTikz(meshImprover.getTriangles(), colorFunction, 1.0f, getTopography()));
			});
			t.start();
		}
	}

	public Collection<VTriangle> getTriangles() {
		if(triangles == null) {
			return Collections.EMPTY_LIST;
		}
		return triangles;
	}*/
}
