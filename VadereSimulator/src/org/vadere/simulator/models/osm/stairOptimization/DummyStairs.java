package org.vadere.simulator.models.osm.stairOptimization;

import java.util.ArrayList;

import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

@Deprecated
public class DummyStairs {
	private VRectangle stairs = new VRectangle(0, 0, 10, 10);
	private ArrayList<VLine> singleStairs = null;
	private int nrStairs = 34; // ~ 30cm/Stair

	public DummyStairs() {
		singleStairs = getSingleStairs();
	}

	public VShape getShape() {
		return stairs;
	}

	public ArrayList<VLine> getSingleStairs() {
		ArrayList<VLine> singleStairs = new ArrayList<>();

		double height = stairs.getHeight();
		double stepDepth = height / nrStairs;

		double curX = stairs.getMinX();
		double curY = stairs.getMinY();

		for (int i = 0; i <= nrStairs; i++) {
			singleStairs.add(new VLine(curX, curY, curX + stairs.getWidth(), curY));
			curY += stepDepth;
		}
		return singleStairs;
	}

	public static void main(String[] args) {
		new DummyStairs();

	}
}
