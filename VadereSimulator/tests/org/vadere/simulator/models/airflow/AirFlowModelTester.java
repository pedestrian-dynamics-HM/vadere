package org.vadere.simulator.models.airflow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class AirFlowModelTester extends AirFlowModel {

    Random rand = new Random();

    @Override
    protected void calculateAirFlow() {
        System.out.println("Calculating air flow...");
        File file1 = new File(airFlow.getScenarioPath() + "_" + airFlow.getScenarioHash() + X_VELOCITY_FILE_ENDING);
        File file2 = new File(airFlow.getScenarioPath() + "_" + airFlow.getScenarioHash() + Y_VELOCITY_FILE_ENDING);
        try {
            if (file1.createNewFile() && file2.createNewFile()) {
                System.out.println("Created files and files");
                PrintWriter w1 = new PrintWriter(new FileWriter(file1));
                PrintWriter w2 = new PrintWriter(new FileWriter(file2));
                w1.println("# 10 10");
                w2.println("# 10 10");
                for (int i = 0; i < 10; i++) {
                    StringBuilder line1 = new StringBuilder();
                    StringBuilder line2 = new StringBuilder();
                    for (int j = 0; j < 10; j++) {
                        line1.append(rand.nextDouble());
                        line1.append(" ");
                        line2.append(rand.nextDouble());
                        line2.append(" ");
                    }
                    w1.println(line1);
                    w2.println(line2);
                }
                w1.close();
                w2.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
