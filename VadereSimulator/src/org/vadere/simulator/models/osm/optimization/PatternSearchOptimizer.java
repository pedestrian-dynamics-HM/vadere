package org.vadere.simulator.models.osm.optimization;


import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.optimization.PatternSearch;

import java.awt.*;
import java.util.List;
import java.util.Random;

/**
 * This is a implementation of https://en.wikipedia.org/wiki/Pattern_search_(optimization).
 * It is not jet tested extensively!
 *
 * @author Benedikt Zoennchen
 */
public class PatternSearchOptimizer extends StepCircleOptimizer {

	private final double distanceThreshold;
	private final AttributesOSM attributesOSM;
	private final List<VPoint> basePoint;
	private final double edgeLen = 0.3;

	public PatternSearchOptimizer(final double distanceThreshold, @NotNull final AttributesOSM attributesOSM, @NotNull final Random random) {
		this.distanceThreshold = distanceThreshold;
		this.attributesOSM = attributesOSM;
		this.basePoint = GeometryUtils.getDiscDiscretizationGridPoints(new VCircle(new VPoint(0,0), 1.0 ), 1.0 / 2);

		//this.basePoint = GeometryUtils.getDiscDiscretizationPoints(random, false, new VCircle(new VPoint(0,0), 1.0), attributesOSM.getNumberOfCircles(), attributesOSM.getStepCircleResolution(), 0, 2*Math.PI);
	}

	@Override
	public VPoint getNextPosition(PedestrianOSM ped, Shape reachableArea) {
		assert reachableArea instanceof VCircle;
		VCircle circle = (VCircle) reachableArea;
		//PatternSearch patternSearch = new PatternSearch(circle, pos -> ped.getPotential(pos), 0.01, basePoint, attributesOSM.getNumberOfCircles(), attributesOSM.getStepCircleResolution());
		PatternSearch patternSearch = new PatternSearch(circle, pos -> ped.getPotential(pos), distanceThreshold, basePoint, circle.getRadius());
		patternSearch.optimize();
		return patternSearch.getArgMin();
	}


	@Override
	public StepCircleOptimizer clone() {
		return this;
	}
}

