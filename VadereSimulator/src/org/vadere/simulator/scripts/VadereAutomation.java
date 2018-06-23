package org.vadere.simulator.scripts;
import java.util.*;
import java.lang.*;

import org.vadere.simulator.control.SourceController;
import org.vadere.simulator.entrypoints.ScenarioBuilder;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.simulator.projects.SingleScenarioFinishedListener;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.Random;


/* Works only for three targets */

public class VadereAutomation {


    //public static final String SCENARIO_NAME = "Kreuzung3_Unit";
    //public static final String SCENARIO_PATH = "D:/repo_checkout/PersMarionGoedel/material/canwelearn/data/";
    public static final String SCENARIO_NAME = "Kreuzung_softShell_overlapping_source";
    public static final String SCENARIO_PATH = "C:/Studium/BA/vadereProjects/";
    public static final int N_SIMULATIONS = 100;
    public static final int N_CONCURENT_SIMULATIONS = 20; //if this is choosen too high, out of memory errors may occur
    public static Scenario final_scenario;
    private static ArrayList<Thread> arrThreads = new ArrayList<Thread>();

    // ThreadPoolExecuter !



    public static void main(String[] args) {

        long start_time = System.currentTimeMillis();

        // load project and choose the scenario
        try {
            VadereProject project = IOVadere.readProject(SCENARIO_PATH);

            BlockingQueue<Scenario> scenarioList = project.getScenarios();

            int n_scenarios = scenarioList.size(); // may change by applying .take()
            Scenario scenario = null;
            for (int j = 0; j < n_scenarios; j++) {
                scenario = scenarioList.take();
                if (scenario.getName().equals(SCENARIO_NAME)) {
                    break;
                }
            }

            if (scenario==null) {
                throw new IllegalArgumentException("Scenario " + SCENARIO_NAME + " does not exist!");
            }

            final_scenario = scenario;

        }catch(Exception e){
            System.out.println("error" + e.getMessage());
        }

        // run simulations
        int amount = N_SIMULATIONS;

        int numberOfIterations = (N_SIMULATIONS / N_CONCURENT_SIMULATIONS) + ((N_SIMULATIONS % N_CONCURENT_SIMULATIONS == 0) ? 0 : 1);

        for (int iteration = 0; iteration < numberOfIterations; iteration++) {

            int nSimulationsLeft = Math.min(N_CONCURENT_SIMULATIONS, amount);
            for (int i = 0; i < nSimulationsLeft; i++) {
                System.out.println("Start of " + (i + iteration * N_CONCURENT_SIMULATIONS));
                startAutomatic(final_scenario);
                amount--;
            }

            // wait for all threads to finish
            try {
                for (int i = 0; i < arrThreads.size(); i++) {
                    arrThreads.get(i).join();
                }
            } catch (InterruptedException ie) {
                System.out.println(ie);
            }
        }
        // time
        long stop_time = (System.currentTimeMillis() - start_time ) / 1000; // seconds
        System.out.println("*** Simulations took " + stop_time + " seconds ****");
    }

    public static int randomCalc(int min, int max) {
        Random rand = new Random();
        int randomNumber = rand.nextInt(max-min)+min;
        return randomNumber;
    }



    public static void startAutomatic(Scenario scenario) { // Scenario als final übergeben
        try {
            ScenarioBuilder builder = new ScenarioBuilder(scenario);


            int spawnNumber = 0;
            for (int i = 0; i < 3; i++) {
                spawnNumber += scenario.getTopography().getSources().get(i).getAttributes().getSpawnNumber();
            }

            int []spawnValues = new int[3];
            Random rand = new Random();
            int randomStart = rand.nextInt()%3;

            if(randomStart < 0){
                randomStart += 3;
            }
            spawnValues[(randomStart)] = randomCalc(0, spawnNumber);
            spawnValues[(randomStart + 1) % 3] = randomCalc(0, spawnNumber - spawnValues[(randomStart)]);
            spawnValues[(randomStart + 2) % 3] = spawnNumber - (spawnValues[(randomStart + 1) % 3] + spawnValues[(randomStart)]);

            builder.setSourceField("spawnNumber", 1, spawnValues[0]);
            builder.setSourceField("spawnNumber", 2, spawnValues[1]);
            builder.setSourceField("spawnNumber", 3, spawnValues[2]);

            String target_distribution = Integer.toString((int) ((float) spawnValues[0] / (float) spawnNumber * 100)) + "-" +
                    Integer.toString((int) ((float) spawnValues[1] / (float) spawnNumber * 100)) + "-" +
                    Integer.toString((int) ((float) spawnValues[2] / (float) spawnNumber * 100));
            System.out.println("Distribution of pedestrians on the targets: " + target_distribution);
            /*
            int spawnNumberLeft = randomCalc(0, spawnNumber);
            int spawnNumberStraight = randomCalc(0, spawnNumber - spawnNumberLeft);
            int spawnNumberRight = spawnNumber - (spawnNumberStraight + spawnNumberLeft);


            builder.setSourceField("spawnNumber", 1, spawnNumberLeft);
            builder.setSourceField("spawnNumber", 2, spawnNumberStraight);
            builder.setSourceField("spawnNumber", 3, spawnNumberRight);


            String target_distribution = Integer.toString((int) ((float) spawnNumberLeft / (float) spawnNumber * 100)) + "-" +
                    Integer.toString((int) ((float) spawnNumberStraight / (float) spawnNumber * 100)) + "-" +
                    Integer.toString((int) ((float) spawnNumberRight / (float) spawnNumber * 100));
            System.out.println("Distribution of pedestrians on the targets: " + target_distribution);
            */

            scenario = builder.build();
            scenario.saveChanges();
            scenario.setName(target_distribution + "_Distribution");
            //scenario.setName("x_Distribution");



            Thread thread = new Thread(new ScenarioRun(scenario, s -> System.out.print(s + "finished")));
            thread.start();
            arrThreads.add(thread);


            //Process pr = rt.exec("java -jar /Users/Do/Documents/Vadere/Vadere.jar");
        } catch (Exception e) {
            System.out.println("error" + e.getMessage());
        }
    }
}

