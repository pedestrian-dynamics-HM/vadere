package org.vadere.simulator.scripts;
import java.util.*;

import org.vadere.simulator.entrypoints.ScenarioBuilder;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.ScenarioRun;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOVadere;

import java.nio.file.Paths;
import java.util.List;


public class VadereAutomation_old {

    public static final int SIMULATION_RUNS = 4;

    public static void main(String[] args) {
        for (int i =0; i < SIMULATION_RUNS; i++) {
            System.out.println("Start of " + i);
            startAutomatic();
        }
    }
    public static int randomCalc(int min, int max) {
        Random rand = new Random();
        int randomNumber = rand.nextInt(max-min)+min;
        return randomNumber;
    }


    Scenario scene1 = (Scenario) Paths.get("D:/marion/Arbeit/repo_checkout/PersMarionGoedel/material/canwelearn/data/scenarios/Kreuzung1.scenario");


    public static void startAutomatic() {
        try {
            // load project
            VadereProject project = IOVadere.readProject("D:/marion/Arbeit/repo_checkout/PersMarionGoedel/material/canwelearn/data/");
            VadereProject projectJson = IOVadere.readProjectJson("D:/marion/Arbeit/repo_checkout/PersMarionGoedel/material/canwelearn/data/");

            Scenario scenario = project.getScenario(0);
            // Scenario JsonScene = projectJson.getScenario(1);

            //ScenarioBuilder builder = new ScenarioBuilder(JsonScene);
            ScenarioBuilder builder2 = new ScenarioBuilder(scenario);

            /*
            JsonScene.getTopography().getSources().get(0).getAttributes().getId();
            builder.setSourceField("id", -1, 2);
            builder.setSourceField("spawnNumber", 2,5);
*/
            int spawnNumber =0;
            for (int i = 0; i <3; i++) {
                spawnNumber += scenario.getTopography().getSources().get(i).getAttributes().getSpawnNumber();
            }

            int spawnNumber1 = 0;
            int spawnNumber2 = 0;
            int spawnNumber3 = 0;

            while (Math.max(Math.max(spawnNumber1, spawnNumber2), spawnNumber3) == 0) {

                // Number of pedestrians assigned to the targets
                spawnNumber1 = randomCalc(0, spawnNumber);
                spawnNumber2 = randomCalc(0, spawnNumber - spawnNumber1);
                spawnNumber3 = spawnNumber - (spawnNumber2 + spawnNumber1);

            }

            builder2.setSourceField("spawnNumber",1,spawnNumber1);
            builder2.setSourceField("spawnNumber",2,spawnNumber2);
            builder2.setSourceField("spawnNumber",3,spawnNumber3);
            String distribution = Integer.toString((int) ((float) spawnNumber1/ (float) spawnNumber*100))+"-"
                    + Integer.toString((int) ((float) spawnNumber2/ (float) spawnNumber*100))+"-"
                    +Integer.toString((int) ((float) spawnNumber3/ (float) spawnNumber*100));
            System.out.println("***************************************** Distribution: " + distribution);

            //JsonScene = builder.build();
            //JsonScene.saveChanges();

            scenario = builder2.build();
            scenario.saveChanges();
            scenario.setName(distribution+"_Distribution");

            //org.vadere.util.io.IOUtils.writeTextFile("",JsonScene);

            // Run the scenario
            //new Thread(new ScenarioRun(scenario, s -> System.out.print(s + "finished"))).start();
            new Thread(new ScenarioRun(scenario, s -> System.out.print(s + "finished"))).start();

            //Process pr = rt.exec("java -jar /Users/Do/Documents/Vadere/Vadere.jar");
        } catch (Exception e) {
            System.out.println("error" + e.getMessage());
        }
    }
}

