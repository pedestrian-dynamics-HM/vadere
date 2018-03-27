package org.vadere.simulator.scripts;

import org.vadere.simulator.entrypoints.ScenarioBuilder;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class VadereAutomationBode {


    public static final String SCENARIO_NAME = "Kreuzung";
    public static final int N_SIMULATIONS = 2;

    public static void main(String[] args) {
        int amount = N_SIMULATIONS;
        for (int i =0; i < amount; i++) {
            System.out.println("Start of simulation " + i);
            startAutomatic();
        }
    }

    public static int randomCalc(int min, int max) {
        Random rand = new Random();
        int randomNumber = min + rand.nextInt(max-min);
        return randomNumber;
    }



    public static void startAutomatic() {
        try {

            // Load a VADERE project
            VadereProject project = IOVadere.readProject("../data/");

            // Find all scenarios within the project
            BlockingQueue<Scenario> scenarioList = project.getScenarios();

            // Find predefined scenario
            int n_scenarios = scenarioList.size();
            Scenario scenario = null;
            for (int i = 0;  i < n_scenarios; i++){
                scenario = scenarioList.take();
                if(scenario.getName().equals(SCENARIO_NAME)) {
                    break;
                }
            }

            if (scenario == null){
                throw new IllegalArgumentException("Scenario " + SCENARIO_NAME + " does not exist!");
            }


            ScenarioBuilder builder = new ScenarioBuilder(scenario);

            // Find number of pedestrians for each source
            int spawnNumber =0;
            for (int i = 0; i < 3; i++) {
                spawnNumber += scenario.getTopography().getSources().get(i).getAttributes().getSpawnNumber();
            }

            // Randomly assign the pedestrians to the sources
            int spawnNumberLeft = randomCalc(0,spawnNumber);
            int spawnNumberStraight = randomCalc(0,spawnNumber-spawnNumberLeft);
            int spawnNumberRight = spawnNumber-(spawnNumberStraight+spawnNumberLeft);

            // Set number of pedestrians in new scenario
            builder.setSourceField("spawnNumber",1,spawnNumberLeft);
            builder.setSourceField("spawnNumber",2,spawnNumberStraight);
            builder.setSourceField("spawnNumber",3,spawnNumberRight);

            String target_distribution = Integer.toString((int)((float)spawnNumberLeft/(float)spawnNumber*100))+"-"+
                    Integer.toString((int)((float)spawnNumberStraight/(float)spawnNumber*100))+"-"+
                    Integer.toString((int)((float)spawnNumberRight/(float)spawnNumber*100));


            // Save new scenario
            scenario = builder.build();
            scenario.saveChanges();
            scenario.setName(target_distribution+"_Distribution");



            // Run the scenario
            new Thread(new ScenarioRun(scenario, s -> System.out.print(s + "finished"))).start();

        } catch (Exception e) {
            System.out.println("error" + e.getMessage());
        }
    }
}



