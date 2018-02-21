package org.vadere.simulator.scripts;
import java.util.*;
import java.lang.*;
import org.vadere.simulator.entrypoints.ScenarioBuilder;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.Random;


/* Works only for three targets */

public class VadereAutomation {


    public static final String SCENARIO_NAME = "Kreuzung2_Unit";
    public static final String SCENARIO_PATH = "D:/repo_checkout/PersMarionGoedel/material/canwelearn/data/";
    public static final int N_SIMULATIONS = 20;
    public static Scenario final_scenario;



    public static void main(String[] args) {

        Random random = new Random();


        List<Integer> value = new LinkedList<>();
        for(int i = 0; i < 1000; i++){
            value.add(random.nextInt(3)+1);
        }

        System.out.println(value);


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
            System.out.println("Start of " + i);
            startAutomatic(final_scenario);
        }
    }

    public static int randomCalc(int min, int max) {
        Random rand = new Random();
        int randomNumber = rand.nextInt(max-min)+min;
        return randomNumber;
    }



    public static void startAutomatic(Scenario scenario) { // Scenario als final übergeben
        try {
            ScenarioBuilder builder = new ScenarioBuilder(scenario);

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

            //JsonScene = builder.build();
            //JsonScene.saveChanges();

            scenario = builder.build();
            scenario.saveChanges();
            scenario.setName(target_distribution+"_Distribution");

            //org.vadere.util.io.IOUtils.writeTextFile("",JsonScene);


            //new Thread(new ScenarioRun(scenario, s -> System.out.print(s + "finished"))).start();
            new Thread(new ScenarioRun(scenario, s -> System.out.print(s + "finished"))).start();

            //Process pr = rt.exec("java -jar /Users/Do/Documents/Vadere/Vadere.jar");
        } catch (Exception e) {
            System.out.println("error" + e.getMessage());
        }
    }
}

