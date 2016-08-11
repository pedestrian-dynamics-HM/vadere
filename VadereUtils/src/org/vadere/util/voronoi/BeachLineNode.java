package org.vadere.util.voronoi;

interface BeachLineNode {
	BeachLineInternal getParent();

	void setParent(BeachLineInternal parent);
}
