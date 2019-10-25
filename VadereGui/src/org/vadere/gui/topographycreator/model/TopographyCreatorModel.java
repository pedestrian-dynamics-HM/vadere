package org.vadere.gui.topographycreator.model;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.model.DefaultConfig;
import org.vadere.gui.components.model.DefaultModel;
import org.vadere.simulator.projects.Scenario;
import org.vadere.state.attributes.scenario.AttributesTopography;
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
import java.awt.geom.Rectangle2D.Double;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observer;
import java.util.function.Predicate;


/**
 * The data of the DrawPanel. Its holds the whole data of one scenario.
 * 
 */
public class TopographyCreatorModel extends DefaultModel implements IDrawPanelModel {

	/**
	 * container of all ScenarioElements, separates the real panelModel (Topography)
	 * from the PanelModel (that holds information about selection, color, scaling and so on).
	 */
	private TopographyBuilder topographyBuilder;

	/**
	 * this is the scalingfactor for the obstacles, targets and sources (if the user want to scale
	 * the topography).
	 * Not only a view-attribute.
	 */
	private double scalingFactor = 1.0;

	/** a copy of the topography element that will be moved, can be null. */
	private VShape prototypeShape;

	/** the last copied element, can be null. */
	private ScenarioElement copiedElement;

	/** indicate that the prototype is availible != null and visible. */
	private boolean prototypeVisble;

	/** Font to be used to display statistical informations. */
	private final Font font;

	/** the stack of accessed categories */
	private LinkedList<ScenarioElementType> typeAccessStack;

	private ScenarioElementType currentType;

	private Cursor cursor;

	private Color cursorColor;

	private Scenario scenario;

	private Observer scenarioObserver;

	private int boundId;

	public TopographyCreatorModel(final Scenario scenario) {
		this(scenario.getTopography(), scenario);
		setVadereScenario(scenario);
	}

	public TopographyCreatorModel(final Topography topography, final Scenario scenario) {
		super(new DefaultConfig());
		this.font = new Font("Arial", Font.PLAIN, 12);
		this.prototypeVisble = false;
		this.topographyBuilder = new TopographyBuilder(topography);
		this.typeAccessStack = new LinkedList<>();
		this.cursorColor = Color.GRAY;
		this.setViewportBound(topography.getBounds());
		this.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		this.currentType = ScenarioElementType.OBSTACLE;
		this.boundId = 0;
		setVadereScenario(scenario);
	}

	@Override
	public void setVadereScenario(final Scenario scenario) {
		this.scenarioObserver = (model, obj) -> scenario.setTopography(topographyBuilder.build());
		this.scenario = scenario;
		this.addObserver(scenarioObserver);
	}

	@Override
	public Scenario getScenario() {
		scenario.setTopography(topographyBuilder.build());
		return scenario;
	}

	@Override
	public Topography getTopography() {
		return topographyBuilder.build();
	}



	@Override
	public double getBoundingBoxWidth() {
		return topographyBuilder.getAttributes().getBoundingBoxWidth();
	}

	@Override
	public void setTopography(final Topography topography) {
		this.topographyBuilder = new TopographyBuilder(topography);
		if (scenarioObserver != null) {
			super.deleteObserver(scenarioObserver);
		}
		this.setVadereScenario(scenario);
		setChanged();
		fireChangeViewportEvent(
				new Rectangle2D.Double(0, 0, topography.getBounds().width, topography.getBounds().height));
	}

	// TODO [priority=low] [task=feature] reimpl
	@Override
	public void scaleTopography(final double scale) {
		/*
		 * scalingFactor = scale;
		 * double topographyError = boundaryWidth - (boundaryWidth * scalingFactor);
		 * Set<Entry<String, ShapeCategory<ScenarioShape>>> entrySet = shapes.entrySet();
		 * for (Entry<String, ShapeCategory<ScenarioShape>> entry : entrySet) {
		 * for (ScenarioShape ss : entry.getValue()) {
		 * if (entry.getKey().equals("pedestrians")
		 * || (entry.getKey().equals("sources") && ss.getShape() instanceof Ellipse2D.Double)) {
		 * ss.setPosition(ss.getX() * scalingFactor + topographyError, ss.getY() * scalingFactor + topographyError);
		 * } else {
		 * ss.scale(scalingFactor, scalingFactor);
		 * ss.setPosition(ss.getX() + topographyError, ss.getY() + topographyError);
		 * }
		 * }
		 * }
		 * 
		 * 
		 * setScenarioBound(new Rectangle2D.Double(scenarioBound.x * scalingFactor, scenarioBound.y
		 * * scalingFactor,
		 * scenarioBound.width * scalingFactor + 2 * topographyError, scenarioBound.height * scalingFactor + 2
		 * * topographyError));
		 * // boundaryWidth *= scalingFactor; // not supported in vadere!
		 * setChanged();
		 */
	}

	@Override
	public void switchType(final ScenarioElementType type) {
		cursorColor = getScenarioElementColor(type);
		currentType = type;
		setChanged();
	}

	/**
	 * Deletes the last added shape if any shape was added before, otherwise
	 * nothing will happen.
	 */
	@Override
	public ScenarioElement deleteLastShape() {
		return deleteLastShape(typeAccessStack.removeLast());
	}

	/**
	 * Deletes the last added shape, if it there (otherwise nothing will happen)
	 * specified by the categoryKey and the type.
	 *
	 * @param type the shape type
	 */
	@Override
	public ScenarioElement deleteLastShape(final ScenarioElementType type) {

		ScenarioElement element;
		switch (type) {
			case OBSTACLE:
				element = topographyBuilder.removeLastObstacle();
				break;
			case PEDESTRIAN:
				element = topographyBuilder.removeLastPedestrian();
				break;
			case SOURCE:
				element = topographyBuilder.removeLastSource();
				break;
			case TARGET:
				element = topographyBuilder.removeLastTarget();
				break;
			case TARGET_CHANGER:
				element = topographyBuilder.removeLastTargetChanger();
				break;
			case ABSORBING_AREA:
				element = topographyBuilder.removeLastAbsorbingArea();
				break;
			case TELEPORTER:
				element = topographyBuilder.removeTeleporter();
				break;
			case STAIRS:
				element = topographyBuilder.removeLastStairs();
				break;
			default:
				throw new IllegalArgumentException("wrong ScenarioElementType.");
		}

		setChanged();
		if (element.equals(getSelectedElement())) {
			selectedElement = null;
		}
		return element;
	}

	@Override
	public boolean isPrototypeVisble() {
		return prototypeShape != null && prototypeVisble;
	}

	@Override
	public boolean isSelectionVisible() {
		return getSelectionShape() != null && super.isSelectionVisible();
	}

	@Override
	public Color getCursorColor() {
		return cursorColor;
	}

	@Override
	public Font getFont() {
		return font;
	}

	@Override
	public void setTopographyBound(final VRectangle scenarioBound) {
		try {
			Field field = AttributesTopography.class.getDeclaredField("bounds");
			field.setAccessible(true);
			field.set(topographyBuilder.getAttributes(), scenarioBound);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {

			e.printStackTrace();
		}
		boundId++;
		calculateScaleFactor();
		setChanged();
	}

	@Override
	public Double getTopographyBound() {
		return topographyBuilder.getAttributes().getBounds();
	}

	@Override
	public void hidePrototypeShape() {
		prototypeVisble = false;
		setChanged();
	}

	@Override
	public void showPrototypeShape() {
		prototypeVisble = true;
		setChanged();
	}


	@Override
	public void resetTopographySize() {
		fireChangeViewportEvent(new VRectangle(topographyBuilder.getAttributes().getBounds()));
	}

	@Override
	public void resetScenario() {
		setTopography(new Topography());
	}

	@Override
	public void addShape(final ScenarioElement shape) {
		switch (shape.getType()) {
			case OBSTACLE:
				topographyBuilder.addObstacle((org.vadere.state.scenario.Obstacle) shape);
				break;
			case STAIRS:
				topographyBuilder.addStairs((org.vadere.state.scenario.Stairs) shape);
				break;
			case PEDESTRIAN:
				topographyBuilder.addPedestrian((AgentWrapper) shape);
				break;
			case SOURCE:
				topographyBuilder.addSource((org.vadere.state.scenario.Source) shape);
				break;
			case TARGET:
				topographyBuilder.addTarget((org.vadere.state.scenario.Target) shape);
				break;
			case TARGET_CHANGER:
				topographyBuilder.addTargetChanger((org.vadere.state.scenario.TargetChanger) shape);
				break;
			case ABSORBING_AREA:
				topographyBuilder.addAbsorbingArea((org.vadere.state.scenario.AbsorbingArea) shape);
				break;
			case TELEPORTER:
				topographyBuilder.setTeleporter((org.vadere.state.scenario.Teleporter) shape);
				break;
			case MEASUREMENT_AREA:
				topographyBuilder.addMeasurementArea((org.vadere.state.scenario.MeasurementArea) shape);
				break;
			default:
				throw new IllegalArgumentException("unsupported TopograpyhElementType.");
		}
		typeAccessStack.add(shape.getType());
		setChanged();
	}


	@Override
	public boolean removeElement(final ScenarioElement element) {
		boolean removed = topographyBuilder.removeElement(element);
		if (element.equals(selectedElement)) {
			selectedElement = null;
		}
		setChanged();
		return removed;

	}

	@Override
	public ScenarioElement removeElement(final VPoint position) {
		// ScenarioElement element = setSelectedElement(position);
		ScenarioElement element = getClickedElement(position);

		if (element != null) {
			topographyBuilder.removeElement(element);
			if (element.equals(selectedElement)) {
				deselectSelectedElement();
			}
			setChanged();
		}

		return element;
	}

	public void translateTopography(final double x, final double y) {
		double oldX = getTopographyBound().x;
		double oldY = getTopographyBound().y;
		translateElements(x - oldX, y - oldY);
		setTopographyBound(new VRectangle(x, y, getTopographyBound().getWidth(), getTopographyBound().getHeight()));
		setChanged();
	}

	public void translateElements(final double dx, final double dy) {
		topographyBuilder.translateElements(dx, dy);
		setChanged();
	}

	@Override
	public VShape resize(final Point start, final Point end) {
		VPoint startVector = translateVectorCoordinates(start);
		VPoint endVector = translateVectorCoordinates(end);
		return getSelectedElement().getShape().resize(startVector, endVector);
	}

	@Override
	public VShape translate(final Point vector) {
		VPoint worldVector = new VPoint(vector.x / getScaleFactor(), -vector.y / getScaleFactor());
		return translate(worldVector);
	}

	@Override
	public VShape translate(final VPoint vector) {
		return translateElement(getSelectedElement(), vector);
	}

	@Override
	public VShape translateElement(ScenarioElement element, VPoint vector) {
		// double factor = Math.max(10,1/getGridResulution()); // ?? related to scaleTopography?
		return element.getShape().translatePrecise(alignToGrid(vector));
	}

	@Override
	public List<Obstacle> getObstacles() {
		return topographyBuilder.getObstacles();
	}

	@Override
	public List<MeasurementArea> getMeasurementAreas(){
		return topographyBuilder.getMeasurementAreas();
	}

	@Override
	public Double getBounds() {
		return topographyBuilder.getAttributes().getBounds();
	}

	@Override
	public void removeObstacleIf(@NotNull final Predicate predicate) {
		if(selectedElement instanceof Obstacle) {
			selectedElement = null;
		}

		topographyBuilder.removeObstacleIf(predicate);
		setChanged();
	}

	@Override
	public void removeMeasurementAreaIf(final @NotNull Predicate predicate){
		if (selectedElement instanceof MeasurementArea){
			selectedElement = null;
		}
		topographyBuilder.removeMeasurementAreaIf(predicate);
		setChanged();
	}

	@Override
	public int getBoundId() {
		return boundId;
	}

	@Override
	public void setViewportBound(final Double viewportBound) {
		Rectangle2D.Double oldViewportBound = this.getViewportBound();
		super.setViewportBound(viewportBound);

		if (!oldViewportBound.equals(viewportBound)) {
			boundId++;
		}
	}

	@Override
	public VShape getPrototypeShape() {
		return prototypeShape;
	}

	@Override
	public void setPrototypeShape(final VShape prototypeShape) {
		this.prototypeShape = prototypeShape;
	}

	@Override
	public void setCursorColor(final Color cursorColor) {
		this.cursorColor = cursorColor;
		setChanged();
	}

	@Override
	public Cursor getCursor() {
		return cursor;
	}

	@Override
	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
		setChanged();
	}

	@Override
	public void setMouseSelectionMode(final IMode selectionMode) {
		super.setMouseSelectionMode(selectionMode);
		setCursor(selectionMode.getCursor());
	}

	@Override
	public ScenarioElement getCopiedElement() {
		return copiedElement;
	}

	@Override
	public void setCopiedElement(final ScenarioElement copiedElement) {
		this.copiedElement = copiedElement;
	}

	@Override
	public double getScalingFactor() {
		return scalingFactor;
	}

	@Override
	public void setScalingFactor(final double scalingFactor) {
		this.scalingFactor = scalingFactor;
	}

	@Override
	public Teleporter getTeleporter() {
		return topographyBuilder.getTeleporter();
	}

	@Override
	public void setTeleporter(Teleporter teleporter) {
		this.topographyBuilder.setTeleporter(teleporter);
	}

	@Override
	public Iterator<ScenarioElement> iterator() {
		return topographyBuilder.iterator();
	}

	@Override
	public ScenarioElementType getCurrentType() {
		return currentType;
	}

	@Override
	public Topography build() {
		return topographyBuilder.build();
	}

	private VPoint alignToGrid(final VPoint point) {
		double factor = Math.max(10, 1 / getGridResolution());
		return new VPoint(Math.round(point.x * factor) / factor, Math.round(point.y * factor) / factor);
	}

}
