package org.vadere.gui.projectview.view;


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
import java.util.LinkedList;

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
        JPanel main;
        JScrollPane scrollPane;
        JPanel btnPane;
        LinkedList<SimulationResult> data;


        ResultDialog(ProjectView projectView, LinkedList<SimulationResult> data) {
            super(projectView);
            this.data = data;
            main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.PAGE_AXIS));

            table = new JTable(getData(data), data.getFirst().getHeaders());
            table.setFillsViewportHeight(true);
            table.doLayout();
            scrollPane = new JScrollPane(table);
            main.add(scrollPane);

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
            c.add(main, BorderLayout.CENTER);
            c.add(btnPane, BorderLayout.PAGE_END);

            setTitle(Messages.getString("ProjectView.label.simResults"));
            setSize(600, 200);

        }


        Object[][] getData(LinkedList<SimulationResult> data) {
            Object[][] res = new Object[data.size()][5];
            int rowIdx = 0;
            for (SimulationResult d : data) {
                res[rowIdx] = d.getAsTableRow();
                rowIdx++;
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
