package org.vadere.meshing.mesh.triangulation.improver.distmesh.deprecated;

import org.vadere.util.math.IDistanceFunction;
import org.vadere.meshing.mesh.triangulation.edgeLengthFunctions.IEdgeLengthFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;
import java.util.function.Function;

public class EdgeLenFunctionFactory {
	enum Method {
		UNIFORM, DISTMESH, DENSITY
	}

	static IEdgeLengthFunction create(final VRectangle regionBoundingBox,
	                                  final Function<IPoint, Double> densityFunc){
		return new DensityEdgeLenFunction(densityFunc, regionBoundingBox);
	}


	static IEdgeLengthFunction create(final VRectangle regionBoundingBox,
	                                  final Collection<? extends VShape> obstacles,
	                                  final IDistanceFunction distanceFunc){
		return new DistanceEdgeLenFunction(regionBoundingBox, obstacles, distanceFunc);
	}

	static IEdgeLengthFunction create(final VRectangle regionBoundingBox,
	                                  final IDistanceFunction distanceFunc){
		return new SimpleDistanceEdgeLenFunction(regionBoundingBox, distanceFunc);
	}

	static IEdgeLengthFunction create(double factor) {return vertex -> factor * (1+vertex.getX()); }

	static IEdgeLengthFunction create(){
		return vertex -> 1.0;
	}
}
