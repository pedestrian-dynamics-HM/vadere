package org.vadere.geometry.mesh.triangulation.adaptive;

import org.vadere.geometry.shapes.IPoint;

import java.util.function.Function;

@FunctionalInterface
public interface IEdgeLengthFunction extends Function<IPoint,Double> {}
