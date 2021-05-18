package org.vadere.simulator.control.external.reaction;


import org.apache.commons.math3.util.Precision;
import org.junit.Test;
import org.vadere.util.io.IOUtils;

import java.io.IOException;

import static org.junit.Assert.*;

public class ReactionModelTest {

    @Test
    public void test_default() throws Exception {

        ReactionModel reactionModel = new ReactionModel();
        assertTrue(reactionModel.isPedReact());

    }

    @Test
    public void test_default_2() throws Exception {

        ReactionModel reactionModel = new ReactionModel(0.0);
        assertFalse(reactionModel.isPedReact());

    }

    @Test
    public void test_default_mean() throws Exception {

        double probability = 0.66;

        ReactionModel reactionModel = new ReactionModel(probability);
        double mean = getDistributionMean(reactionModel,10000,0);

        assertTrue( Precision.equals(mean, probability, 0.02));

    }


    @Test
    public void testOneOptionOnly() throws Exception {

        String dataPath = "testResources/control/external/ReactionProbabilities2.json";
        String data = "";
        int trials = 100000;

        try {
            data = IOUtils.readTextFile(dataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ReactionModel reactionModel = new ReactionModel(data);
        double mean = getDistributionMean(reactionModel, trials, 0);

        assertTrue( Precision.equals(mean, 0.75, 0.02));

        // TODO: check standard deviation
        //double sum = counter*Math.pow(1.0-mean, 2) + (trials-counter)*Math.pow(0-mean, 2);
        //double std = Math.pow( 1.0/trials * sum  ,0.5);

    }

    private double getDistributionMean(ReactionModel reactionModel, int trials, int index) {
        boolean isReact;
        int counter = 0;
        for (int i = 0; i <= trials; i++) {
            try {
                if (reactionModel.isPedReact(index)) {
                    counter += 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return counter * 1.0 / trials;
    }



    private String getStringFromFile(){
        String dataPath = "testResources/control/external/ReactionProbabilities.json";
        String data = "";

        try {
            data = IOUtils.readTextFile(dataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }


    @Test
    public void testMultipleOptionsFailure(){

        String data = getStringFromFile();

        ReactionModel reactionModel = new ReactionModel(data);
        try {
            reactionModel.isPedReact();
            fail();
        }
        catch (Exception e){
           assertEquals(e.getMessage(), "There are 4 reaction behavior probabilities defined. Please choose an option.");
        }

    }


    @Test
    public void testMultipleOptions(){

        String data = getStringFromFile();
        int trials = 100000;

        ReactionModel reactionModel = new ReactionModel(data);
        boolean isPedReact;


        try {
            assertEquals(reactionModel.isPedReact(0), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            assertEquals(reactionModel.isPedReact(1), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue( Precision.equals(getDistributionMean(reactionModel, trials, 2), 0.5, 0.02));
        assertTrue( Precision.equals(getDistributionMean(reactionModel, trials, 3), 0.8, 0.02));

    }




}
