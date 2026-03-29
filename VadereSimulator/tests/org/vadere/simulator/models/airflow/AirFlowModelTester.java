package org.vadere.simulator.models.airflow;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.AirFlow;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Random;

public class AirFlowModelTester extends AirFlowModel {

    String airflowVxText = """
            # 5_5_2.0-0.1-1.0-left[1.0,3.0]-right[4.0,9.0]-[]
            -1.256216669569004907e+00 1.387209395208421903e-01 1.006659986516889038e-01 5.910834297269129678e-02 1.205509911898217101e-02
            -1.192013101139092424e+00 1.356572263676802947e-01 1.120789486323658934e-01 1.018515102501893477e-01 2.813687969180633797e-02
            -9.318573899846267494e-01 6.090815244078617852e-02 8.981733991324514221e-02 1.051175984832453114e-01 1.982127966814850595e-01
            -8.553889098492584164e-01 2.788846152597113104e-02 5.174776639026990033e-02 6.219576920042380053e-02 7.532079748470144054e-02
            -8.338185670537996419e-01 2.094453202539092729e-02 3.812727199513643583e-02 4.934078995009749669e-02 3.965734927056652204e-02
            """;

    String airflowVyText = """
            # 5_5_2.0-0.1-1.0-left[1.0,3.0]-right[4.0,9.0]-[]
            -1.261126620801137133e+00 -9.274923103412759939e-01 -6.901492516888998452e-01 -5.232308795891127406e-01 -4.470652918109071505e-01
            2.310359304858677909e-01 3.082918998166261915e-02 -7.351838868280280437e-03 9.082772955748552590e-03 9.543054118449634249e-02
            5.962064969353697563e-02 3.860173298083635629e-02 -6.929141154676798919e-03 -4.138890865393318741e-02 -1.366114436637810847e-01
            2.114568160496155258e-02 1.489460873540515706e-02 -3.925748335292800206e-03 -2.938940473896090033e-02 -6.476611644872232176e-02
            4.180337738451944674e-03 3.333853042031265090e-03 -4.026288308112135894e-03 -1.106773514173986506e-02 -1.717588413264192759e-02
            """;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        super.initialize(attributesList, domain, attributesPedestrian, random);
        this.attributesAirFlowModel = Model.findAttributes(attributesList, AttributesAirFlowModel.class);
        String scenarioPath = this.airFlow.getScenarioPath();
        this.airFlow = new AirFlow(scenarioPath, "test_hash", 0, 0, 2, 2);
    }

    @Override
    protected void calculateAirFlow(String hash) {
        // This overrides the parent's actual calculation method.
        // Instead of running heavy Python scripts during testing,
        // we "calculate" by just writing hardcoded test strings to the cache.
        // Write files to the same location that setupAirFlow() will look for them.
        // setupAirFlow() constructs: cacheDir / scenarioName + "_" + hash + ending
        File scenarioFile = new File(airFlow.getScenarioPath());
        File cacheDir = new File(scenarioFile.getParent(), "cache");
        cacheDir.mkdirs();
        String scenarioName = scenarioFile.getName().replaceFirst("\\.scenario$", "");

        File file1 = new File(cacheDir, scenarioName + "_" + hash + X_VELOCITY_FILE_ENDING);
        File file2 = new File(cacheDir, scenarioName + "_" + hash + Y_VELOCITY_FILE_ENDING);
        try {
            file1.createNewFile();
            file2.createNewFile();
            PrintWriter w1 = new PrintWriter(new FileWriter(file1));
            PrintWriter w2 = new PrintWriter(new FileWriter(file2));
            w1.print(airflowVxText);
            w2.print(airflowVyText);
            w1.close();
            w2.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void mockMalformedAirFlowFile() {
        // Simulates a corrupted or malformed airflow file.
        // This writes only the header with no actual velocity matrix data.
        File scenarioFile = new File(airFlow.getScenarioPath());
        File cacheDir = new File(scenarioFile.getParent(), "cache");
        cacheDir.mkdirs();
        String scenarioName = scenarioFile.getName().replaceFirst("\\.scenario$", "");

        File file1 = new File(cacheDir, scenarioName + "_" + airFlow.getAirflowHash() + X_VELOCITY_FILE_ENDING);
        File file2 = new File(cacheDir, scenarioName + "_" + airFlow.getAirflowHash() + Y_VELOCITY_FILE_ENDING);
        try {
            file1.createNewFile();
            file2.createNewFile();
            PrintWriter w1 = new PrintWriter(new FileWriter(file1));
            PrintWriter w2 = new PrintWriter(new FileWriter(file2));
            w1.println("# 5_5_3.0-0.1-0.5-left[1.0,2.0]-right[4.0,5.0]-[]");
            w2.println("# 5_5_3.0-0.1-0.5-left[1.0,2.0]-right[4.0,5.0]-[]");
            w1.close();
            w2.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
