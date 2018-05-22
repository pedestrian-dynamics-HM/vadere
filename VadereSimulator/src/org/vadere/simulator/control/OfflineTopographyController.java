package org.vadere.simulator.control;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.vadere.simulator.models.potential.fields.IPotentialField;
import org.vadere.simulator.models.potential.fields.ObstacleDistancePotential;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

public class OfflineTopographyController {

	private final Topography topography;

	public OfflineTopographyController(final Topography topography) {
		this.topography = topography;
	}

	protected void update(double simTimeInSec) {
		recomputeCells();
	}

	public Topography getTopography() {
		return topography;
	}

	// add bounding box
	protected void prepareTopography() {
		// add boundaries
		if (this.topography.isBounded() && !this.topography.hasBoundary()) {
			VPolygon boundary = new VPolygon(this.topography.getBounds());
			double width = this.topography.getBoundingBoxWidth();
			Collection<VPolygon> boundingBoxObstacleShapes = boundary
					.borderAsShapes(width, width / 2.0, 0.0001);
			for (VPolygon obstacleShape : boundingBoxObstacleShapes) {
				AttributesObstacle obstacleAttributes = new AttributesObstacle(
						-1, obstacleShape);
				Obstacle obstacle = new Obstacle(obstacleAttributes);
				this.topography.addBoundary(obstacle);
			}
		}

		// add distance function
		IPotentialField distanceField = new ObstacleDistancePotential(
				topography.getObstacles().stream().map(obs -> obs.getShape()).collect(Collectors.toList()),
				new VRectangle(topography.getBounds()),
				new AttributesFloorField());
		Function<VPoint, Double> obstacleDistance = p -> distanceField.getPotential(p, null);
		this.topography.setObstacleDistanceFunction(obstacleDistance);
	}

	/**
	 * Recomputes the {@link org.vadere.util.geometry.LinkedCellsGrid} for fast access to pedestrian
	 * and car
	 * neighbors.
	 */
	protected void recomputeCells() {
		this.topography.getSpatialMap(Pedestrian.class).clear();
		for (Pedestrian pedestrian : this.topography.getElements(Pedestrian.class)) {
			this.topography.getSpatialMap(Pedestrian.class).addObject(pedestrian,
					pedestrian.getPosition());
		}
		this.topography.getSpatialMap(Car.class).clear();
		for (Car car : this.topography.getElements(Car.class)) {
			this.topography.getSpatialMap(Car.class).addObject(car,
					car.getPosition());
		}
	}
}
