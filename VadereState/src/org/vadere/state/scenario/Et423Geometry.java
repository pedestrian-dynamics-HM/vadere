package org.vadere.state.scenario;

/**
 * This is the S-Bahn train known from Munich and other (German) cities.
 * 
 * @see https://de.wikipedia.org/wiki/DB-Baureihe_423
 * @see http://www.nahverkehr-franken.de/sbahn/img_techdat/423_zeichnung.jpg
 * @see http://de.bombardier.com/content/dam/Websites/bombardiercom/Projects/technical-drawings/et-423-electric%20multiple%20unit-techdraw.gif
 * 
 *
 */
public class Et423Geometry extends TrainGeometry {

	@Override
	public double getDoorWidth() {
		return 1.3;
	}

	@Override
	public double getEntranceAreaWidth() {
		return 1.65;
	}

	@Override
	public double getBenchWidth() {
		return 0.965;
	}

	@Override
	public double getDistanceBetweenFacingBenches() {
		return 0.545;
	}

	@Override
	public double getAisleWidth() {
		return 0.825;
	}

	@Override
	public double getAisleEntranceWidth() {
		return 0.72;
	}

	@Override
	public double getAisleLength() {
		// 3 entrance areas + 3 compartments (according to the plan):
		// (15.460 + 2.700 / 2 - 3 * getEntranceAreaWidth()) / 3.0
		return 2 * 1.65;
	}

}
