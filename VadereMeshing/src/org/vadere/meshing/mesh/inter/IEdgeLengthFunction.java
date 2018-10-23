package org.vadere.meshing.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.function.Function;

@FunctionalInterface
public interface IEdgeLengthFunction extends Function<IPoint,Double> {}
