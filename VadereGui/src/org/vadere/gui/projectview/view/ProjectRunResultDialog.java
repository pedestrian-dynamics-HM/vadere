package org.vadere.gui.projectview.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.simulator.projects.ProjectFinishedListener;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.VadereProject;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

import javax.swing.*;

public class ProjectRunResultDialog implements ProjectFinishedListener {
	private static Logger logger = LogManager.getLogger(ProjectRunResultDialog.class);

	private final ProjectView projectView;
	private final ProjectViewModel projectViewModel;

	public ProjectRunResultDialog(ProjectView projectView, ProjectViewModel projectViewModel) {
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
			sb.append(res.getScenarioName()).append(":\n")
					.append("    Runtime: ").append(res.getRunTimeAsString()).append("\n")
					.append("    Overlaps: ").append(res.getTotalOverlaps()).append("\n")
					.append("    State: ").append(res.getState()).append("\n\n");
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
		private final String[] columnNames = {"Scenario_Name",
				"Runtime",
				"Overlaps",
				"State"};
		Button btnOk, btnCsv;
		private JTable table;
		JPanel main;
		JScrollPane scrollPane;
		JPanel btnPane;
		LinkedList<SimulationResult> data;


		public ResultDialog(ProjectView projectView, LinkedList<SimulationResult> data) {
			super(projectView);
			this.data = data;
			main = new JPanel();
			main.setLayout(new BoxLayout(main, BoxLayout.PAGE_AXIS));

			table = new JTable(getData(data), columnNames);
			table.setFillsViewportHeight(true);
			table.doLayout();
			scrollPane = new JScrollPane(table);
			main.add(scrollPane);

			btnOk = new Button("Close");
			btnOk.addActionListener(this::btnOKListener);
			btnCsv = new Button("Export csv");
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

			setTitle("Simulation Result");
			setSize(600, 200);

		}


		public Object[][] getData(LinkedList<SimulationResult> data) {
			Object[][] res = new Object[data.size()][4];
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
			SimulationResult.addCsvHeader(sj, ';');
			data.forEach(i -> i.addCsvRow(sj, ';'));


			FileDialog fd = new FileDialog(this, "Bitte eine Datei waehlen!", FileDialog.SAVE);

			fd.setVisible(true);
			Path p = (Paths.get(fd.getDirectory()).resolve(fd.getFile()));


			fd.setVisible(false);

			try (OutputStreamWriter writer =
						 new OutputStreamWriter(new FileOutputStream(p.toString(), false), StandardCharsets.UTF_8)) {
				writer.write(sj.toString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			setVisible(false);
		}

	}
}
