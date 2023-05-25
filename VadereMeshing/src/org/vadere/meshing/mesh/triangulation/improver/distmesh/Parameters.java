package org.vadere.meshing.mesh.triangulation.improver.distmesh;

/** @author Matthias Laubinger */
public class Parameters {
  public static final double DPTOL = 0.001;
  public static final double TOL = .1;
  public static final double FSCALE = 1.2;
  public static final double DELTAT = 0.2;
  public static final double MIN_TRIANGLE_QUALITY = 0.1;
  public static final double MIN_FORCE_RATIO = 0.3;
  public static final double MIN_COLLAPSE_QUALITY = 0.5;
  public static final double MIN_SPLIT_QUALITY = 0.7;
  public static final double MAX_COLLAPSE_ANGLE = Math.PI * 0.5;
  public static final double h0 = 0.15;
  public static final boolean uniform = false;
  public static final String method = "Distmesh"; // "Distmesh" or "Density"
  public static final double qualityMeasurement = 0.95;
  public static final double qualityConvergence = 0.0;
  public static final double MINIMUM = 0.25;
  public static final double DENSITYWEIGHT = 2;
  public static final int NPOINTS = 100000;
  public static final int SAMPLENUMBER = 10;
  public static final int SAMPLEDIVISION = 10;
  public static final int SEGMENTDIVISION = 0;
  // TODO increase this
  public static final int MAX_NUMBER_OF_STEPS = 200;
  public static final int HIGHEST_LEGAL_TEST = Integer.MAX_VALUE;
}
