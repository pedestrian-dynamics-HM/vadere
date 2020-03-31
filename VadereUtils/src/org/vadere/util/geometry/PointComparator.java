package org.vadere.util.geometry;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.Comparator;

public class PointComparator<T extends IPoint> implements Comparator<T> {

	@Override
	public int compare(@NotNull final T o1, @NotNull final T o2) {
		if(o1.getX() < o2.getX()) {
			return -1;
		} else if(o1.getX() > o2.getX()) {
			return 1;
		} else {
			if(o1.getY() < o2.getY()) {
				return -1;
			} else if(o1.getY() > o2.getY()) {
				return 1;
			} else {
				return 0;
			}
		}
	}

}
