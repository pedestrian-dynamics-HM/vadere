package org.vadere.util.geometry.mesh.inter;

import org.vadere.util.geometry.shapes.IPoint;

/**
 * @author Benedikt Zoennchen
 */
public interface IHierarchyPoint extends IPoint {
	void setDown(IHierarchyPoint down);
	void setUp(IHierarchyPoint up);
}
