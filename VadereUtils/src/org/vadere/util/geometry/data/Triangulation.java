package org.vadere.util.geometry.data;

import org.vadere.util.geometry.shapes.IPoint;

import java.util.Set;
import java.util.stream.Stream;

public interface Triangulation<P extends IPoint> {
	Face<P> locate(P point);
	Stream<Face<P>> streamFaces();
	Set<Face<P>> getFaces();
	void insert(P point);
	void remove(P point);
}
