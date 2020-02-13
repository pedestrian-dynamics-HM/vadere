package org.vadere.simulator.utils.pslg;

import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.impl.PSLG;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PSLGConverter {

	public PSLG toPSLG(@NotNull final Topography topography, final int targetId) {
		Rectangle2D.Double boundWithBorder = topography.getBounds();
		double boundWidth = topography.getBoundingBoxWidth();
		VRectangle bound = new VRectangle(boundWithBorder.x + boundWidth, boundWithBorder.y + boundWidth,
				boundWithBorder.width - 2*boundWidth, boundWithBorder.height - 2*boundWidth);

		List<Obstacle> obstacles = new ArrayList<>(topography.getObstacles());
		obstacles.removeAll(topography.getBoundaryObstacles());

		List<VPolygon> obsShapes = obstacles.stream()
				.map(obs -> obs.getShape())
				.map(shape -> new VPolygon(shape))
				.collect(Collectors.toList());

		obsShapes.addAll(topography.getTargets(targetId).stream().map(target -> target.getShape()).map(shape -> new VPolygon(shape)).collect(Collectors.toList()));

		// this computes the union of intersecting obstacles.
		obsShapes = PSLG.constructHoles(obsShapes);

		// this will help to construct a valid non-rectangular bound.
		List<VPolygon> polygons = PSLG.constructBound(new VPolygon(bound), obsShapes);

		return new PSLG(polygons.get(0), polygons.size() > 1 ? polygons.subList(1, polygons.size()) : Collections.emptyList());
	}

	public PSLG toPSLG(@NotNull final Topography topography) {
		Rectangle2D.Double boundWithBorder = topography.getBounds();
		double boundWidth = topography.getBoundingBoxWidth();
		VRectangle bound = new VRectangle(boundWithBorder.x + boundWidth, boundWithBorder.y + boundWidth,
				boundWithBorder.width - 2*boundWidth, boundWithBorder.height - 2*boundWidth);

		List<Obstacle> obstacles = new ArrayList<>(topography.getObstacles());
		obstacles.removeAll(topography.getBoundaryObstacles());

		List<VPolygon> obsShapes = obstacles.stream()
				.map(obs -> obs.getShape())
				.map(shape -> new VPolygon(shape))
				.collect(Collectors.toList());

		// this computes the union of intersecting obstacles.
		obsShapes = PSLG.constructHoles(obsShapes);

		// this will help to construct a valid non-rectangular bound.
		List<VPolygon> polygons = PSLG.constructBound(new VPolygon(bound), obsShapes);

		return new PSLG(polygons.get(0), polygons.size() > 1 ? polygons.subList(1, polygons.size()) : Collections.emptyList());
	}
}
