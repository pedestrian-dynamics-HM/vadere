package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.VTriangle;

import java.util.Collection;

/**
 * Created by bzoennchen on 13.09.17.
 */
public interface IPSMeshing {

    Collection<VTriangle> getTriangles();

}
