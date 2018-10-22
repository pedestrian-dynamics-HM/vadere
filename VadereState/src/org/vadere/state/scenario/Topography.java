package org.vadere.state.scenario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.geometry.shapes.IPoint;
import org.vadere.geometry.shapes.VPoint;
import org.vadere.geometry.shapes.VPolygon;
import org.vadere.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.PathFindingTag;

@JsonIgnoreProperties(value = {"allOtherAttributes", "obstacleDistanceFunction"})
public class Topography {

	/** Transient to prevent JSON serialization. */
	private static Logger logger = Logger.getLogger(Topography.class);

	private Function<IPoint, Double> obstacleDistanceFunction;
	
	// TODO [priority=low] [task=feature] magic number, use attributes / parameter?
	/**
	 * Cell size of the internal storage of DynamicElements. Is used in the LinkedCellsGrid.
	 */
	private static final double CELL_SIZE = 2;

	private final AttributesTopography attributes;

	/**
	 * Obstacles of scenario by id. Tree maps ensures same update order during
	 * iteration between frames.
	 */
	private final List<Obstacle> obstacles;
	/**
	 * Sources of scenario by id. Tree maps ensures same update order during
	 * iteration between frames.
	 */
	private final List<Source> sources;
	/**
	 * Targets of scenario by id. Tree maps ensures same update order during
	 * iteration between frames.
	 */
	private final LinkedList<Target> targets;

	/**
	 * List of obstacles used as a boundary for the whole topography.
	 */
	private List<Obstacle> boundaryObstacles;

	private final List<Stairs> stairs;

	private Teleporter teleporter;

	private transient final DynamicElementContainer<Pedestrian> pedestrians;
	private transient final DynamicElementContainer<Car> cars;

	private AttributesAgent attributesPedestrian;
	private AttributesCar attributesCar;

	/** Used to get attributes of all scenario elements. */
	private Set<List<? extends ScenarioElement>> allScenarioElements = new HashSet<>(); // will be filled in the constructor
	
	/** Used to store links to all attributes that are not part of scenario elements. */
	private Set<Attributes> allOtherAttributes = new HashSet<>(); // will be filled in the constructor

	public Topography(
			AttributesTopography attributes,
			AttributesAgent attributesPedestrian,
			AttributesCar attributesCar) {

		this.attributes = attributes;
		this.attributesPedestrian = attributesPedestrian;
		this.attributesCar = attributesCar;

		allOtherAttributes.add(attributes);
		allOtherAttributes.add(attributesCar);
		allOtherAttributes.add(attributesPedestrian);
		removeNullFromSet(allOtherAttributes);
		// Actually, only attributes, not nulls should be added to this set.
		// But sometimes null is passed as attributes and added to the set,
		// although it is bad practice to pass null in the first place
		// (as constructor argument).

		obstacles = new LinkedList<>();
		stairs = new LinkedList<>();
		sources = new LinkedList<>();
		targets = new LinkedList<>();
		boundaryObstacles = new LinkedList<>();
		
		allScenarioElements.add(obstacles);
		allScenarioElements.add(stairs);
		allScenarioElements.add(sources);
		allScenarioElements.add(targets);
		allScenarioElements.add(boundaryObstacles);

		RectangularShape bounds = this.getBounds();

		this.pedestrians = new DynamicElementContainer<>(bounds, CELL_SIZE);
		this.cars = new DynamicElementContainer<>(bounds, CELL_SIZE);

		this.obstacleDistanceFunction = p -> obstacles.stream().map(obs -> obs.getShape()).map(shape -> shape.distance(p)).min(Double::compareTo).orElse(Double.MAX_VALUE);
		
	}

	/** Clean up a set by removing {@code null}. */
	private void removeNullFromSet(Set<?> aSet) {
		aSet.remove(null);
	}

	public Topography() {
		this(new AttributesTopography(), new AttributesAgent(), new AttributesCar());
	}

	public Rectangle2D.Double getBounds() {
		return this.attributes.getBounds();
	}

	public double getBoundingBoxWidth() {
		return this.attributes.getBoundingBoxWidth();
	}

	public Target getTarget(int targetId) {
		for (Target target : this.targets) {
			if (target.getId() == targetId) {
				return target;
			}
		}

		return null;
	}

	public double distanceToObstacle(@NotNull IPoint point) {
		return this.obstacleDistanceFunction.apply(point);
	}

	public void setObstacleDistanceFunction(@NotNull Function<IPoint, Double> obstacleDistanceFunction) {
		this.obstacleDistanceFunction = obstacleDistanceFunction;
	}

	public boolean containsTarget(final Predicate<Target> targetPredicate) {
		return getTargets().stream().anyMatch(targetPredicate);
	}

	public boolean containsTarget(final Predicate<Target> targetPredicate, final int targetId) {
		return getTargets().stream().filter(t -> t.getId() == targetId).anyMatch(targetPredicate);
	}

	/**
	 * Returns a list containing Targets with the specific id. This list may be empty.
	 */
	public List<Target> getTargets(final int targetId) {
		return getTargets().stream().filter(t -> t.getId() == targetId).collect(Collectors.toList());
	}

	public Map<Integer, List<VShape>> getTargetShapes() {
		return getTargets().stream()
				.collect(Collectors
						.groupingBy(t -> t.getId(), Collectors
								.mapping(t -> t.getShape(), Collectors
										.toList())));
	}

	@SuppressWarnings("unchecked")
	private <T extends DynamicElement, TAttributes extends AttributesDynamicElement> DynamicElementContainer<T> getContainer(
			Class<? extends T> elementType) {
		if (Car.class.isAssignableFrom(elementType)) {
			return (DynamicElementContainer<T>) cars;
		}
		if (Pedestrian.class.isAssignableFrom(elementType)) {
			return (DynamicElementContainer<T>) pedestrians;
		}
		// TODO [priority=medium] [task=refactoring] this is needed for the SimulationDataWriter. Refactor in the process of refactoring the Writer.
		if (DynamicElement.class.isAssignableFrom(elementType)) {

			DynamicElementContainer result = new DynamicElementContainer<>(this.getBounds(), CELL_SIZE);
			for (Pedestrian ped : pedestrians.getElements()) {
				result.addElement(ped);
			}
			for (Car car : cars.getElements()) {
				result.addElement(car);
			}
			return result;
		}

		throw new IllegalArgumentException("Class " + elementType + " does not have a container.");
	}

	public <T extends DynamicElement> LinkedCellsGrid<T> getSpatialMap(Class<T> elementType) {
		return getContainer(elementType).getCellsElements();
	}

	public <T extends DynamicElement> Collection<T> getElements(Class<T> elementType) {
		return getContainer(elementType).getElements();
	}

	public <T extends DynamicElement> T getElement(Class<T> elementType, int id) {
		return getContainer(elementType).getElement(id);
	}

	public <T extends DynamicElement> void addElement(T element) {
		((DynamicElementContainer<T>) getContainer(element.getClass())).addElement(element);
	}

	public <T extends DynamicElement> void removeElement(T element) {
		((DynamicElementContainer<T>) getContainer(element.getClass())).removeElement(element);
	}

	public <T extends DynamicElement> void moveElement(T element, final VPoint oldPosition) {
		((DynamicElementContainer<T>) getContainer(element.getClass())).moveElement(element, oldPosition);
	}

	public List<Source> getSources() {
		return sources;
	}

	public List<Target> getTargets() {
		return targets;
	}

	public List<Obstacle> getObstacles() {
		return obstacles;
	}

	public List<Stairs> getStairs() {
		return stairs;
	}

	public Teleporter getTeleporter() {
		return teleporter;
	}

	public DynamicElementContainer<Pedestrian> getPedestrianDynamicElements() {
		return pedestrians;
	}

	public DynamicElementContainer<Car> getCarDynamicElements() {
		return cars;
	}

	public void addSource(Source source) {
		this.sources.add(source);
	}

	public void addTarget(Target target) {
		this.targets.add(target);
	}

	public void addObstacle(Obstacle obstacle) {
		this.obstacles.add(obstacle);
	}

	public void addStairs(Stairs stairs) {
		this.stairs.add(stairs);
	}

	public void setTeleporter(Teleporter teleporter) {
		allScenarioElements.remove(this.teleporter); // remove old teleporter

		this.teleporter = teleporter;
		if (teleporter != null)
			allScenarioElements.add(Collections.singletonList(teleporter));
	}

	public <T extends DynamicElement> void addInitialElement(T element) {
		@SuppressWarnings("unchecked") // getContainer returns a correctly parameterized object
		final DynamicElementContainer<T> container = (DynamicElementContainer<T>) getContainer(element.getClass());
		container.addInitialElement(element);
	}

	public <T extends DynamicElement> List<T> getInitialElements(Class<T> elementType) {
		return this.getContainer(elementType).getInitialElements();
	}

	public boolean hasTeleporter() {
		return teleporter != null;
	}

	public AttributesTopography getAttributes() {
		return attributes;
	}

	public AttributesAgent getAttributesPedestrian() {
		return attributesPedestrian;
	}

	public void setAttributesPedestrian(AttributesAgent attributesPedestrian) {
		this.attributesPedestrian = attributesPedestrian;
	}

	public AttributesCar getAttributesCar() {
		return attributesCar;
	}

	public void setAttributesCar(AttributesCar attributesCar) {
		this.attributesCar = attributesCar;
	}

	public <T extends DynamicElement> void addElementRemovedListener(Class<T> elementType,
			DynamicElementRemoveListener<T> listener) {
		getContainer(elementType).addElementRemovedListener(listener);
	}

	public <T extends DynamicElement> void clearListeners(Class<T> elementType) {
		getContainer(elementType).clearListeners();
	}

	public <T extends DynamicElement> void addElementAddedListener(Class<T> elementType,
			DynamicElementAddListener<T> addListener) {
		getContainer(elementType).addElementAddedListener(addListener);
	}

	/**
	 * Adds a given obstacle to the list of obstacles as well as the list of boundary obstacles.
	 * This way, the boundary can both be treated like normal obstacles, but can also be removed for
	 * writing the topography to file.
	 */
	public void addBoundary(Obstacle obstacle) {
		this.addObstacle(obstacle);
		this.boundaryObstacles.add(obstacle);
	}

	public void removeBoundary() {
		for (Obstacle boundaryObstacle : this.boundaryObstacles) {
			this.obstacles.remove(boundaryObstacle);
		}
		this.boundaryObstacles.clear();
	}

	/**
	 * Call this method to reset the topography to the state before a simulation take place.
	 * After this call all generated boundaries, pedestrians (from source) and all listeners will be
	 * removed.
	 */
	public void reset() {
		removeBoundary();
		pedestrians.clear();
		cars.clear();
		clearListeners(Pedestrian.class);
		clearListeners(Car.class);
	}

	public boolean isBounded() {
		return this.attributes.isBounded();
	}

	/**
	 * Creates a deep copy of the scenario.
	 * 
	 * @deprecated This manual implementation is error-prone. Remove this method
	 *             and use the standard clone instead.
	 */
	@Deprecated
	@Override
	public Topography clone() {
		Topography s = new Topography(this.attributes, this.attributesPedestrian, this.attributesCar);

		for (Obstacle obstacle : this.getObstacles()) {
			if (boundaryObstacles.contains(obstacle))
				s.addBoundary(obstacle.clone());
			else
				s.addObstacle(obstacle.clone());
		}
		for (Stairs stairs : getStairs()) {
			s.addStairs(stairs);
		}
		for (Target target : getTargets()) {
			s.addTarget(target.clone());
		}
		for (Source source : getSources()) {
			s.addSource(source.clone());
		}
		for (Pedestrian pedestrian : getElements(Pedestrian.class)) {
			s.addElement(pedestrian);
		}
		for (Pedestrian ped : getInitialElements(Pedestrian.class)) {
			s.addInitialElement(ped);
		}
		for (Car car : getElements(Car.class)) {
			s.addElement(car);
		}
		for (Car car : getInitialElements(Car.class)) {
			s.addInitialElement(car);
		}

		if (hasTeleporter()) {
			s.setTeleporter(teleporter.clone());
		}

		for (DynamicElementAddListener<Pedestrian> pedestrianAddListener : this.pedestrians.getElementAddedListener()) {
			s.addElementAddedListener(Pedestrian.class, pedestrianAddListener);
		}
		for (DynamicElementRemoveListener<Pedestrian> pedestrianRemoveListener : this.pedestrians
				.getElementRemovedListener()) {
			s.addElementRemovedListener(Pedestrian.class, pedestrianRemoveListener);
		}
		for (DynamicElementAddListener<Car> carAddListener : this.cars.getElementAddedListener()) {
			s.addElementAddedListener(Car.class, carAddListener);
		}
		for (DynamicElementRemoveListener<Car> carRemoveListener : this.cars.getElementRemovedListener()) {
			s.addElementRemovedListener(Car.class, carRemoveListener);
		}

		return s;
	}

	public int getNextFreeTargetID() {
		Collections.sort(this.targets);
		return targets.getLast().getId() + 1;
	}

	public int getNearestTarget(VPoint position) {
		double distance = Double.MAX_VALUE;
		double tmpDistance;
		int targetID = -1;

		for (Target target : this.targets) {
			if (!target.isTargetPedestrian()) {
				tmpDistance = target.getShape().distance(position);
				if (tmpDistance < distance) {
					distance = tmpDistance;
					targetID = target.getId();
				}
			}
		}

		return targetID;
	}

	public boolean hasBoundary() {
		return this.boundaryObstacles.size() > 0;
	}

	public void sealAllAttributes() {
		// tried to do this with flatMap -> weird compiler error "cannot infer type arguments ..."
		for (List<? extends ScenarioElement> list : allScenarioElements) {

			// defensive programming:
			if (list == null)
				throw new RuntimeException("scenario elem list is null");
			for (ScenarioElement scenarioElement : list) {
				if (scenarioElement.getAttributes() == null) {
					list.remove(scenarioElement);
					logger.warn("a scenario element has null as attributes: " + scenarioElement);
					// TODO this is an error in a different place and should be fixed!
				}
			}

			list.forEach(se -> se.getAttributes().seal());
		}
		allOtherAttributes.forEach(a -> a.seal());
	}

	public void generateUniqueIdIfNotSet(){
		Set<Integer> usedIds = sources.stream().map(Source::getId).collect(Collectors.toSet());
		usedIds.addAll(targets.stream().map(Target::getId).collect(Collectors.toSet()));
		usedIds.addAll(obstacles.stream().map(Obstacle::getId).collect(Collectors.toSet()));
		usedIds.addAll(stairs.stream().map(Stairs::getId).collect(Collectors.toSet()));

		sources.stream()
				.filter(s -> s.getId() == Attributes.ID_NOT_SET)
				.forEach(s -> s.getAttributes().setId(nextIdNotInSet(usedIds)));

		targets.stream()
				.filter(s -> s.getId() == Attributes.ID_NOT_SET)
				.forEach(s -> s.getAttributes().setId(nextIdNotInSet(usedIds)));

		obstacles.stream()
				.filter(s -> s.getId() == Attributes.ID_NOT_SET)
				.forEach(s -> s.setId(nextIdNotInSet(usedIds)));

		stairs.stream()
				.filter(s -> s.getId() == Attributes.ID_NOT_SET)
				.forEach(s -> s.getAttributes().setId(nextIdNotInSet(usedIds)));


	}

	private int nextIdNotInSet(Set<Integer> usedIDs){
		int newId = 1;
		while (usedIDs.contains(newId)){
			newId++;
		}
		usedIDs.add(newId);
		return newId;
	}


	public ArrayList<ScenarioElement> getAllScenarioElements(){
		ArrayList<ScenarioElement> all = new ArrayList<>((obstacles.size() + stairs.size() + targets.size() + sources.size() + boundaryObstacles.size()));
		all.addAll(obstacles);
		all.addAll(stairs);
		all.addAll(targets);
		all.addAll(sources);
		all.addAll(boundaryObstacles);
		return  all;

	}

	public static Collection<Obstacle> createObstacleBoundary(@NotNull final Topography topography) {
		List<Obstacle> obstacles = new ArrayList<>();
		VPolygon boundary = new VPolygon(topography.getBounds());
		double width = topography.getBoundingBoxWidth();
		Collection<VPolygon> boundingBoxObstacleShapes = boundary.borderAsShapes(width, width / 2.0, 0.0001);

		for (VPolygon obstacleShape : boundingBoxObstacleShapes) {
			AttributesObstacle obstacleAttributes = new AttributesObstacle(
					-1, obstacleShape);
			Obstacle obstacle = new Obstacle(obstacleAttributes);
			obstacles.add(obstacle);
		}

		return obstacles;
	}
}
