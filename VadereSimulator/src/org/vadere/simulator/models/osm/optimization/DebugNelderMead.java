package org.vadere.simulator.models.osm.optimization;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.direct.NelderMead;
import org.vadere.util.logging.Logger;

//import java.io.FileWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;

public class DebugNelderMead extends NelderMead {

    /** Reflection coefficient. */
    private final double rho;

    /** Expansion coefficient. */
    private final double khi;

    /** Contraction coefficient. */
    private final double gamma;

    /** Shrinkage coefficient. */
    private final double sigma;

    //FileWriter fileWriter;

    NelderMead nelderMead;


    public DebugNelderMead() {
        this.rho   = 1.0;
        this.khi   = 2.0;
        this.gamma = 0.5;
        this.sigma = 0.5;



    }

    private static Logger logger = Logger.getLogger(DebugNelderMead.class);


    protected void iterateSimplex(final Comparator<RealPointValuePair> comparator)
            throws FunctionEvaluationException, OptimizationException {

        incrementIterationsCounter();

        // the simplex has n+1 point if dimension is n
        final int n = simplex.length - 1;

        // interesting values
        final RealPointValuePair best       = simplex[0];
        final RealPointValuePair secondBest = simplex[n-1];
        final RealPointValuePair worst      = simplex[n];
        final double[] xWorst = worst.getPointRef();



        // compute the centroid of the best vertices
        // (dismissing the worst point at index n)
        final double[] centroid = new double[n];
        for (int i = 0; i < n; ++i) {
            final double[] x = simplex[i].getPointRef();
            for (int j = 0; j < n; ++j) {
                centroid[j] += x[j];
            }
        }
        final double scaling = 1.0 / n;
        for (int j = 0; j < n; ++j) {
            centroid[j] *= scaling;
        }

        if(StepCircleOptimizerNelderMead.artificialDebugStopForReadOnlyNelderMead == 1){
            logger.info("best (x,y): " + best.toString());
            logger.info("secondBest (x,y): " + secondBest.toString() );
            logger.info("worst (x,y): " + worst.toString() );
            logger.info("Centroid (x,y): " + centroid[0] + ";" + centroid[1]);

            try {

                StepCircleOptimizerNelderMead.fileWriter.write(String.valueOf(StepCircleOptimizerNelderMead.pointCounter));
                StepCircleOptimizerNelderMead.fileWriter.write(";");


                //StepCircleOptimizerNelderMead.fileWriter.write("best; ");
                StepCircleOptimizerNelderMead.fileWriter.write( String.valueOf( best.getPointRef()[0] ) );
                StepCircleOptimizerNelderMead.fileWriter.write( "; " );
                StepCircleOptimizerNelderMead.fileWriter.write( String.valueOf( best.getPointRef()[1] ) );
                StepCircleOptimizerNelderMead.fileWriter.write(";");

                //StepCircleOptimizerNelderMead.fileWriter.write("secondBest; ");
                StepCircleOptimizerNelderMead.fileWriter.write( String.valueOf( secondBest.getPointRef()[0] ) );
                StepCircleOptimizerNelderMead.fileWriter.write( "; " );
                StepCircleOptimizerNelderMead.fileWriter.write( String.valueOf( secondBest.getPointRef()[1] ) );
                StepCircleOptimizerNelderMead.fileWriter.write(";");

                //StepCircleOptimizerNelderMead.fileWriter.write("worst; ");
                StepCircleOptimizerNelderMead.fileWriter.write( String.valueOf( worst.getPointRef()[0] ) );
                StepCircleOptimizerNelderMead.fileWriter.write( "; " );
                StepCircleOptimizerNelderMead.fileWriter.write( String.valueOf( worst.getPointRef()[1] ) );
                StepCircleOptimizerNelderMead.fileWriter.write("\r\n");


            } catch (IOException e) {
                e.printStackTrace();
            }

            logger.info("State :" + StepCircleOptimizerNelderMead.artificialDebugStopForReadOnlyNelderMead );

        }

        // compute the reflection point
        final double[] xR = new double[n];
        for (int j = 0; j < n; ++j) {
            xR[j] = centroid[j] + this.rho * (centroid[j] - xWorst[j]);
        }
        final RealPointValuePair reflected = new RealPointValuePair(xR, evaluate(xR), false);

        if ((comparator.compare(best, reflected) <= 0) &&
                (comparator.compare(reflected, secondBest) < 0)) {

            // accept the reflected point
            replaceWorstPoint(reflected, comparator);

        } else if (comparator.compare(reflected, best) < 0) {

            // compute the expansion point
            final double[] xE = new double[n];
            for (int j = 0; j < n; ++j) {
                xE[j] = centroid[j] + khi * (xR[j] - centroid[j]);
            }
            final RealPointValuePair expanded = new RealPointValuePair(xE, evaluate(xE), false);

            if (comparator.compare(expanded, reflected) < 0) {
                // accept the expansion point
                replaceWorstPoint(expanded, comparator);
            } else {
                // accept the reflected point
                replaceWorstPoint(reflected, comparator);
            }

        } else {

            if (comparator.compare(reflected, worst) < 0) {

                // perform an outside contraction
                final double[] xC = new double[n];
                for (int j = 0; j < n; ++j) {
                    xC[j] = centroid[j] + gamma * (xR[j] - centroid[j]);
                }
                final RealPointValuePair outContracted = new RealPointValuePair(xC, evaluate(xC), false);

                if (comparator.compare(outContracted, reflected) <= 0) {
                    // accept the contraction point
                    replaceWorstPoint(outContracted, comparator);
                    return;
                }

            } else {

                // perform an inside contraction
                final double[] xC = new double[n];
                for (int j = 0; j < n; ++j) {
                    xC[j] = centroid[j] - gamma * (centroid[j] - xWorst[j]);
                }
                final RealPointValuePair inContracted = new RealPointValuePair(xC, evaluate(xC), false);

                if (comparator.compare(inContracted, worst) < 0) {
                    // accept the contraction point
                    replaceWorstPoint(inContracted, comparator);
                    return;
                }

            }

            // perform a shrink
            final double[] xSmallest = simplex[0].getPointRef();
            for (int i = 1; i < simplex.length; ++i) {
                final double[] x = simplex[i].getPoint();
                for (int j = 0; j < n; ++j) {
                    x[j] = xSmallest[j] + sigma * (x[j] - xSmallest[j]);
                }
                simplex[i] = new RealPointValuePair(x, Double.NaN, false);
            }
            evaluateSimplex(comparator);


        }

    }


}
