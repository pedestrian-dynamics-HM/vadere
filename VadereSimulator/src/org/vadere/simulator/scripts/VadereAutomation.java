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

import java.util.concurrent.TimeUnit;


/* Works only for three targets */

public class VadereAutomation {



    //public static final String SCENARIO_NAME = "Kreuzung3_Unit";
    public static final String SCENARIO_NAME = "Kreuzung_Studenten";
    // public static final String SCENARIO_PATH = "D:/marion/Arbeit/repo_checkout/PersMarionGoedel/material/canwelearn/data/";
    public static final String SCENARIO_PATH = "D:/repo_checkout/PersMarionGoedel/material/canwelearn/data/";
    public static final int N_SIMULATIONS = 20;
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
        for (int i =0; i < amount; i++) {
            startAutomatic(final_scenario);
        }

        // wait for all threads to finish
        try {
            for (int i = 0; i < arrThreads.size(); i++) {
                arrThreads.get(i).join();
            }
        }catch(InterruptedException ie){
            System.out.println(ie);
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

            /*
            int spawnNumber =0;
            for (int i = 0; i < 3; i++) {
                spawnNumber += scenario.getTopography().getSources().get(i).getAttributes().getSpawnNumber();
            }


            int spawnNumberLeft = randomCalc(0,spawnNumber);
            int spawnNumberStraight = randomCalc(0,spawnNumber-spawnNumberLeft);
            int spawnNumberRight = spawnNumber-(spawnNumberStraight+spawnNumberLeft);


            builder.setSourceField("spawnNumber",1,spawnNumberLeft);
            builder.setSourceField("spawnNumber",2,spawnNumberStraight);
            builder.setSourceField("spawnNumber",3,spawnNumberRight);

            String target_distribution = Integer.toString((int)((float)spawnNumberLeft/(float)spawnNumber*100))+"-"+
                    Integer.toString((int)((float)spawnNumberStraight/(float)spawnNumber*100))+"-"+
                    Integer.toString((int)((float)spawnNumberRight/(float)spawnNumber*100));
            System.out.println("Distribution of pedestrians on the targets: " +  target_distribution);

            */

            scenario = builder.build();
            scenario.saveChanges();
           //  scenario.setName(target_distribution+"_Distribution");
           scenario.setName("x_Distribution");



            Thread thread = new Thread(new ScenarioRun(scenario, s -> System.out.print(s + "finished")));
            thread.start();
            arrThreads.add(thread);

            TimeUnit.MILLISECONDS.sleep(2); // make sure they don't have the same name


            //Process pr = rt.exec("java -jar /Users/Do/Documents/Vadere/Vadere.jar");
        } catch (Exception e) {
            System.out.println("error" + e.getMessage());
        }
    }
}

