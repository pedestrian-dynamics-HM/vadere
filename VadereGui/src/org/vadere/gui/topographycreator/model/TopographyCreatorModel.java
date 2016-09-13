package org.vadere.gui.topographycreator.model;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observer;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.model.DefaultConfig;
import org.vadere.gui.components.model.DefaultModel;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Teleporter;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;


/**
 * The data of the DrawPanel. Its holds the whole data of one scenario.
 * 
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

	private ScenarioRunManager scenario;

	private Observer scenarioObserver;

	private int boundId;

	public TopographyCreatorModel(final ScenarioRunManager scenario) {
		this(scenario.getTopography(), scenario);
		setVadereScenario(scenario);
	}

	public TopographyCreatorModel(final Topography topography, final ScenarioRunManager scenario) {
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
	public void setVadereScenario(final ScenarioRunManager scenario) {
		this.scenarioObserver = (model, obj) -> scenario.setTopography(topographyBuilder.build());
		this.scenario = scenario;
		this.addObserver(scenarioObserver);
	}

	public ScenarioRunManager getScenario() {
		return scenario;
	}

	@Override
	public Topography getTopography() {
		return topographyBuilder.build();
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
		 * double error = boundaryWidth - (boundaryWidth * scalingFactor);
		 * Set<Entry<String, ShapeCategory<ScenarioShape>>> entrySet = shapes.entrySet();
		 * for (Entry<String, ShapeCategory<ScenarioShape>> entry : entrySet) {
		 * for (ScenarioShape ss : entry.getValue()) {
		 * if (entry.getKey().equals("pedestrians")
		 * || (entry.getKey().equals("sources") && ss.getShape() instanceof Ellipse2D.Double)) {
		 * ss.setPosition(ss.getX() * scalingFactor + error, ss.getY() * scalingFactor + error);
		 * } else {
		 * ss.scale(scalingFactor, scalingFactor);
		 * ss.setPosition(ss.getX() + error, ss.getY() + error);
		 * }
		 * }
		 * }
		 * 
		 * 
		 * setScenarioBound(new Rectangle2D.Double(scenarioBound.x * scalingFactor, scenarioBound.y
		 * * scalingFactor,
		 * scenarioBound.width * scalingFactor + 2 * error, scenarioBound.height * scalingFactor + 2
		 * * error));
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

	/*
	 * @Override
	 * public double getFinishTime() {
	 * return topographyBuilder.getAttributes().getFinishTime();
	 * }
	 */

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
			case TELEPORTER:
				topographyBuilder.setTeleporter((org.vadere.state.scenario.Teleporter) shape);
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

	@Override
	public VShape translate(final Point vector) {
		// double factor = Math.max(10,1/getGridResulution());
		VPoint worldVector = new VPoint(vector.x / getScaleFactor(), -vector.y / getScaleFactor());
		return translate(worldVector);
	}

	@Override
	public VShape translate(final VPoint vector) {
		// double factor = Math.max(10,1/getGridResulution());
		return getSelectedElement().getShape().translatePrecise(alignToGrid(vector));
	}

	// can return null!

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

	// privte helper methods
	private VPoint alignToGrid(final VPoint point) {
		double factor = Math.max(10, 1 / getGridResolution());
		return new VPoint(Math.round(point.x * factor) / factor, Math.round(point.y * factor) / factor);
	}
}
