package org.vadere.meshing.mesh.triangulation.improver.distmesh;

/**
 * @author Matthias Laubinger
 */
public class Parameters {
	public final static double DPTOL = 0.001;
    public final static double TOL = .1;
    public final static double FSCALE = 1.2;
    public final static double DELTAT = 0.2;
    public final static double MIN_TRIANGLE_QUALITY = 0.1;
    public final static double MIN_FORCE_RATIO = 0.3;
    public final static double MIN_COLLAPSE_QUALITY = 0.5;
	public final static double MIN_SPLIT_QUALITY = 0.7;
	public final static double MAX_COLLAPSE_ANGLE = Math.PI * 0.5;
	public final static double h0 = 0.15;
	public final static boolean uniform = false;
	public final static String method = "Distmesh"; // "Distmesh" or "Density"
    public final static double qualityMeasurement = 0.95;
	public final static double qualityConvergence = 0.0;
    public final static double MINIMUM = 0.25;
    public final static double DENSITYWEIGHT = 2;
    public final static int NPOINTS = 100000;
    public final static int SAMPLENUMBER = 10;
    public final static int SAMPLEDIVISION = 10;
    public final static int SEGMENTDIVISION = 0;
    //TODO increase this
    public final static int MAX_NUMBER_OF_STEPS = 200;
	public final static int HIGHEST_LEGAL_TEST = Integer.MAX_VALUE;
}
