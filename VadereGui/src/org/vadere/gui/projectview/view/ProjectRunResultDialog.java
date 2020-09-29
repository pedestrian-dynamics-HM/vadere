package org.vadere.gui.projectview.view;


import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.simulator.projects.ProjectFinishedListener;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.chart.ChartPanel;

import javax.swing.*;

public class ProjectRunResultDialog implements ProjectFinishedListener {
    private static Logger logger = Logger.getLogger(ProjectRunResultDialog.class);

    private final ProjectView projectView;
    private final ProjectViewModel projectViewModel;

    ProjectRunResultDialog(ProjectView projectView, ProjectViewModel projectViewModel) {
        this.projectView = projectView;
        this.projectViewModel = projectViewModel;
    }

    @Override
    public void preProjectRun(VadereProject project) {

    }

    @Override
    public void postProjectRun(VadereProject project) {
        LinkedList<SimulationResult> simulationResultList = project.getSimulationResults();
        StringBuilder sb = new StringBuilder();

        for (SimulationResult res : simulationResultList) {
            String[] headers = res.getHeaders();
            String[] values = res.getAsTableRow();

            for (int i = 0; i < headers.length; i++) {
                sb.append("    ").append(headers[i]).append(": ").append(values[i]).append("\n");
            }
        }


        if (projectViewModel.isShowSimulationResultDialog()) {
            SwingUtilities.invokeLater(() -> {
                JDialog dialog = new ResultDialog(projectView, simulationResultList);
                dialog.setVisible(true);

            });
        } else {
            logger.info(sb.toString());
        }

    }

    class ResultDialog extends JDialog {
        Button btnOk, btnCsv;
        private JTable table;
        JPanel main_panel;
        JPanel plots_panel;
        JScrollPane scrollPane;
        JPanel btnPane;
        LinkedList<SimulationResult> data;


        ResultDialog(ProjectView projectView, LinkedList<SimulationResult> data) {
            super(projectView);
            this.data = data;
            main_panel = new JPanel();
            main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.PAGE_AXIS));

            table = new JTable(getData(data), data.getFirst().getHeaders());
            table.setFillsViewportHeight(true);
            table.doLayout();
            scrollPane = new JScrollPane(table);
            main_panel.add(scrollPane);


            // add new panel for results plots
            plots_panel = new JPanel();
            FlowLayout plots = new FlowLayout(); // next to each other
            plots_panel.setLayout(plots);

            JScrollPane sPannel = new JScrollPane(plots_panel);


            // Code histograms

            // collect scenario names that are in the ProjectRunResultDialog
            Set<String> scenarios = data.stream().map(SimulationResult::getScenarioName).collect(Collectors.toSet());
            if( scenarios.size() == 1 && data.size() > 1) { // only one scenario was run, but multiple times

                // merge results from all runs into one MultiValueMap (allows duplicate entries)
                MultiMap mergedResults = new MultiValueMap();
                data.forEach(d -> mergedResults.putAll(d.getData()));
                Iterator iterator =  mergedResults.entrySet().iterator();
                // collect all results for one processor
                while(iterator.hasNext()) {
                    Map.Entry element = (Map.Entry) iterator.next();
                    ArrayList<Double> values = (ArrayList) element.getValue();
                    double[] values_arr = values.stream().mapToDouble(Double::doubleValue).toArray();

                    var dataset = new HistogramDataset();
                    double mean = values.stream().mapToDouble(d->d).sum()/values.size();
                    dataset.addSeries("mean="+mean, values_arr, 10);

                    JFreeChart histogram = ChartFactory.createHistogram((String) element.getKey(),
                            "", "", dataset);
                    XYPlot xyplot = histogram.getXYPlot();
                    xyplot.setForegroundAlpha(0.7F);
                    xyplot.setBackgroundPaint(Color.WHITE);
                    xyplot.setDomainGridlinePaint(new Color(150, 150, 150));
                    xyplot.setRangeGridlinePaint(new Color(150, 150, 150));
                    XYBarRenderer renderer  = (XYBarRenderer) xyplot.getRenderer();
                    renderer.setShadowVisible(false);
                    renderer.setDrawBarOutline(true);
                    renderer.setBarPainter(new StandardXYBarPainter());

                    ChartPanel cp_hist = new ChartPanel(histogram);
                    cp_hist.setPreferredSize(new Dimension(500/mergedResults.size(), 500/mergedResults.size()));
                    plots_panel.add(cp_hist);
                }

            main_panel.add(sPannel);
            }




            btnOk = new Button(Messages.getString("SettingsDialog.btnClose.text"));
            btnOk.addActionListener(this::btnOKListener);
            btnCsv = new Button(Messages.getString("ProjectView.btnExpertCSV"));
            btnPane = new JPanel();
            btnCsv.addActionListener(this::btnCsvListener);
            btnPane.setLayout(new BoxLayout(btnPane, BoxLayout.LINE_AXIS));
            btnPane.add(Box.createHorizontalGlue());
            btnPane.add(btnOk);
            btnPane.add(Box.createRigidArea(new Dimension(10, 0)));
            btnPane.add(btnCsv);

            Container c = getContentPane();
            c.add(main_panel, BorderLayout.CENTER);
            c.add(btnPane, BorderLayout.PAGE_END);

            setTitle(Messages.getString("ProjectView.label.simResults"));
            setSize(600, 400);

        }



        Object[][] getData(LinkedList<SimulationResult> data) {

            int n_rows = data.size();
            Set<String> scenarios = data.stream().map(SimulationResult::getScenarioName).collect(Collectors.toSet());
            if( scenarios.size() == 1 && data.size() > 1) { // only one scenario was run, but multiple times
                n_rows += 5; // for mean, min, max, std
            }


            // todo: adapt for different processors among scenarios or make sure that all scenarios have the same processors defined
            Object[][] res = new Object[n_rows][5]; // todo: replace static limitation of 5 (processors?)
            int rowIdx = 0;
            for (SimulationResult d : data) {
                res[rowIdx] = d.getAsTableRow();
                rowIdx++;
            }

            // collect scenario names that are in the ProjectRunResultDialog
            if( scenarios.size() == 1 && data.size() > 1) { // only one scenario was run, but multiple times
                // merge results from all runs into one MultiValueMap (allows duplicate entries)
                MultiMap mergedResults = new MultiValueMap();
                data.forEach(d -> mergedResults.putAll(d.getData()));


                // iterate over keys
                Iterator it = mergedResults.entrySet().iterator();
                int n = mergedResults.entrySet().size();
                Map<Object, Map> summaryStat = new HashMap();
                String[] stats = new String[]{"min","max","mean","std"};

                while (it.hasNext()) {
                    Map.Entry mapEntry = (Map.Entry) it.next();
                    if(mapEntry.getValue() instanceof ArrayList) {

                        ArrayList tmp_val = (ArrayList) mapEntry.getValue();

                        double min = -1;
                        double max = -1;
                        double mean = -1;
                        double std = -1;
                        HashMap tmp_summary = new HashMap<>();


                        if(tmp_val.get(0) instanceof Double){
                            ArrayList<Double> values = (ArrayList<Double>) mapEntry.getValue(); // todo: what if there is another data type (e.g. Long)

                            min= Collections.min(values);  // min
                            max = Collections.max(values); // max
                            mean= 0;                        // mean
                            std = 0;                        // std

                            for (Double val : values) {
                                mean += val;
                            }
                            mean = mean / values.size();
                            for (Double val : values) {
                                std += (val - mean) * (val - mean);
                            }
                            std = std / (values.size() - 1);
                            std = Math.sqrt(std);
                        }else{
                            System.out.println("ProjectRunResultDialog: Type is not yet supported \n " + tmp_val.get(0).getClass());
                        }

                        tmp_summary.put("mean",mean);
                        tmp_summary.put("min", min);
                        tmp_summary.put("std", std);
                        tmp_summary.put("max", max);
                        summaryStat.put(mapEntry.getKey(), tmp_summary);

                    }else{
                        System.out.println("ProjectRunResultDialog: Type is not yet supported \n " + mapEntry.getValue().getClass());
                    }
                }

                String[] emptyString = new String[n+3];
                java.util.Arrays.fill(emptyString,"");
                res[rowIdx++] = emptyString; // separate summary stat.

                for(int i_stat = 0; i_stat < stats.length; i_stat++){
                    int icol = 0;
                    String[] tmp_stat = new String[n+3];

                    tmp_stat[icol++] = "summaryStats: " + stats[i_stat];
                    tmp_stat[icol++] = "-";
                    for(int i_proc = 2; i_proc < data.getFirst().getHeaders().length-1; i_proc++){
                        tmp_stat[icol++] = summaryStat.get(data.getFirst().getHeaders()[i_proc]).get(stats[i_stat]).toString();
                    }

                    tmp_stat[icol] = "-";
                    res[rowIdx++] = tmp_stat;
                }
            }

            return res;
        }

        private void btnOKListener(ActionEvent actionEvent) {
            setVisible(false);
        }

        private void btnCsvListener(ActionEvent actionEvent) {
            StringBuilder sj = new StringBuilder();
            SimulationResult.addCsvHeader(data.getFirst(), sj, ';');
            data.forEach(simulationResult -> simulationResult.addCsvRow(sj, ';'));

            FileDialog fd = new FileDialog(this, Messages.getString("ProjectView.chooseFile"), FileDialog.SAVE);

            fd.setVisible(true);
            Path p = (Paths.get(fd.getDirectory()).resolve(fd.getFile()));

            fd.setVisible(false);

            try (OutputStreamWriter writer =
                         new OutputStreamWriter(new FileOutputStream(p.toString(), false), StandardCharsets.UTF_8)) {
                writer.write(sj.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
