package org.vadere.util.triangulation.adaptive;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.VTriangle;

public class IndexedVTriangle extends VTriangle {

	public final IndexedVPoint p1;
	public final IndexedVPoint p2;
	public final IndexedVPoint p3;

	/**
	 * Creates a triangle. Points must be given in ccw order.
	 */
	public IndexedVTriangle(@NotNull IndexedVPoint p1, @NotNull IndexedVPoint p2, @NotNull IndexedVPoint p3) {
		super(p1, p2, p3);
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
	}
}
