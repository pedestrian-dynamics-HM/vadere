package org.vadere.meshing.mesh.triangulation.improver.distmesh;

/**
 * @author Matthias Laubinger
 */
public class FixedParameters {
    public final static int NPOINTS = 100000;
    public final static int SAMPLENUMBER = 10;
    public final static int SAMPLEDIVISION = 10;
    public final static double QUALITYMEASUREMENT = 0.95;
    public final static double QUALITYTOLERANCE = 0.00001;
    public final static int SEGMENTDIVISION = 0;
    public final static double TTOL = 0.1;
    public final static double FSCALE = 1.2;
    public final static double DELTAT = 0.2;
    public final static boolean LOG = false;
    public final static double DENSITYWEIGHT = 1.;
    public static final double MINIMUM = 0.5;
    public static final double Parts = 250;
    public static final long TimeLimit = 60000;
}
