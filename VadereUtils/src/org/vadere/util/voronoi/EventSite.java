package org.vadere.util.voronoi;

import org.vadere.util.geometry.shapes.VPoint;

public class EventSite extends Event {

	private static int idCounter = 0;

	private final int siteId;
	private final VPoint site;

	EventSite(VPoint site) {
		super(site.x, site.y, site.y, site.x);
		this.site = site;

		idCounter++;
		this.siteId = idCounter;
	}

	VPoint getSite() {
		return site;
	}

	int getSiteId() {
		return siteId;
	}
}
