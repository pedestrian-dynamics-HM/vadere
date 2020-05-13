package org.vadere.gui.topographycreator.view;


import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.view.ProjectView;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.stream.Collectors;

public class ActionRandomPedestrianDialog {

	public enum TARGET_OPTION { EMPTY, RANDOM, USE_LIST };

	private JTextField numberOfPeds_field;
	private JTextField boundaryRectangle_field;
	private JTextField targets_field;
	private JTextField seed_field;

	private JRadioButton rbTargetEmpty;
	private JRadioButton rbTargetRandom;
	private JRadioButton rbTargetUseList;

	private JPanel panel;

	private boolean valid;
	private int numOfPeds;
	private Rectangle2D.Double boundaryRectangle;
	private LinkedList<Integer> targetList;
	private int seed;
	private Random random;

	public ActionRandomPedestrianDialog() {

		numberOfPeds_field = new JTextField("10", 15);
		numberOfPeds_field.setHorizontalAlignment(JTextField.RIGHT);
		numberOfPeds_field.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				String text = numberOfPeds_field.getText();
				try{
					numOfPeds = Integer.parseInt(text);
					valid = true;
					numberOfPeds_field.setForeground(Color.BLACK);
				} catch (Exception ex){
					valid = false;
					numberOfPeds_field.setForeground(Color.RED);
				}
			}
		});

		boundaryRectangle_field = new JTextField("x, y, width, height", 15);
		boundaryRectangle_field.setHorizontalAlignment(JTextField.RIGHT);
		boundaryRectangle_field.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				String text = boundaryRectangle_field.getText();
				try{
					String[] numbersAsString = text.split(",");

					boundaryRectangle = new Rectangle2D.Double(
							Double.parseDouble(numbersAsString[0]),
							Double.parseDouble(numbersAsString[1]),
							Double.parseDouble(numbersAsString[2]),
							Double.parseDouble(numbersAsString[3])
							);

					valid = true;
					boundaryRectangle_field.setForeground(Color.BLACK);
				} catch (Exception ex){
					valid = false;
					boundaryRectangle_field.setForeground(Color.RED);
				}
			}
		});

		targets_field = new JTextField("-1", 15);
		targets_field.setHorizontalAlignment(JTextField.RIGHT);
		targets_field.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				String strippedText = targets_field.getText().replace(" ", "");
				String[] splittedText = strippedText.split(",");

				try {
					targetList = Arrays.stream(splittedText).mapToInt(Integer::parseInt).boxed().collect(Collectors.toCollection(LinkedList::new));
					valid = true;
					targets_field.setForeground(Color.BLACK);
				}catch (Exception ex){
					valid = false;
					targets_field.setForeground(Color.RED);
				}
			}
		});

		JPanel panelRadioButtons = createTargetRadioButtonPanel();

		seed_field = new JTextField("-1", 15);
		seed_field.setHorizontalAlignment(JTextField.RIGHT);
		seed_field.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				try{
					seed = Integer.parseInt(seed_field.getText());
					valid = true;
					seed_field.setForeground(Color.BLACK);
				} catch (Exception ex){
					valid = false;
					seed_field.setForeground(Color.RED);
				}
			}
		});

		GridBagLayout layout = new GridBagLayout();
		panel = new JPanel();
		panel.setLayout(layout);

		panel.add(new JLabel("Set Number of Pedestrians"), c(GridBagConstraints.HORIZONTAL, 0, 0));
		panel.add(numberOfPeds_field, c(GridBagConstraints.HORIZONTAL, 1, 0));
		panel.add(new JLabel("In Boundary Rectangle"), c(GridBagConstraints.HORIZONTAL, 0, 1));
		panel.add(boundaryRectangle_field, c(GridBagConstraints.HORIZONTAL, 1, 1));

		panel.add(new JLabel("Set Targets"), c(GridBagConstraints.HORIZONTAL, 0, 2));
		panel.add(panelRadioButtons, c(GridBagConstraints.HORIZONTAL, 1, 2));
		panel.add(targets_field, c(GridBagConstraints.HORIZONTAL, 1, 3));

		panel.add(new JLabel("Set Random Seed (-1 for random)"), c(GridBagConstraints.HORIZONTAL, 0, 4));
		panel.add(seed_field, c(GridBagConstraints.HORIZONTAL, 1, 4));
		panel.add(new JLabel("May take a while because intelligent distance function not available yet..."),
				  c(GridBagConstraints.HORIZONTAL, 0,5,2));

		numOfPeds = 10;
		targetList = new LinkedList<>();
		targetList.add(-1);
		seed = -1;
		valid = false;
	}

	@NotNull
	private JPanel createTargetRadioButtonPanel() {
		rbTargetEmpty = new JRadioButton(Messages.getString("TopographyCreator.PlaceRandomPedestrians.targetEmptyOption.label"), true);
		rbTargetRandom = new JRadioButton(Messages.getString("TopographyCreator.PlaceRandomPedestrians.targetRandomOption.label"), false);
		rbTargetUseList = new JRadioButton(Messages.getString("TopographyCreator.PlaceRandomPedestrians.targetListOption.label"), false);
		targets_field.setEditable(false);

		ButtonGroup buttonGroupTarget = new ButtonGroup();
		buttonGroupTarget.add(rbTargetEmpty);
		buttonGroupTarget.add(rbTargetRandom);
		buttonGroupTarget.add(rbTargetUseList);

		rbTargetUseList.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean targetsFieldState = e.getStateChange() == ItemEvent.SELECTED;
				targets_field.setEditable(targetsFieldState);
			}
		});

		JPanel panelRadioButtons = new JPanel();
		panelRadioButtons.setLayout(new FlowLayout());

		int x = 0;
		int y = 0;
		panelRadioButtons.add(rbTargetEmpty, c(GridBagConstraints.HORIZONTAL, x, y++));
		panelRadioButtons.add(rbTargetRandom, c(GridBagConstraints.HORIZONTAL, x, y++));
		panelRadioButtons.add(rbTargetUseList, c(GridBagConstraints.HORIZONTAL, x, y++));
		return panelRadioButtons;
	}


	private GridBagConstraints c(int fill, int gridx, int gridy, int width){

		GridBagConstraints c = new GridBagConstraints();
		c.fill = fill;
		c.gridx = gridx;
		c.gridy = gridy;
		c.gridwidth = width;
		c.insets = new Insets(2,2,2,2);
		return c;
	}

	private GridBagConstraints c(int fill, int gridx, int gridy){
		GridBagConstraints c = new GridBagConstraints();
		c.fill = fill;
		c.gridx = gridx;
		c.gridy = gridy;
		c.insets = new Insets(2,2,2,2);
		return c;
	}

	public int getNumOfPeds() {
		return numOfPeds;
	}

	public Rectangle2D.Double getBoundaryRectangle() { return boundaryRectangle; }

	public TARGET_OPTION getTargetOption() {
		TARGET_OPTION selectedOption;

		if (rbTargetEmpty.isSelected()) {
			selectedOption = TARGET_OPTION.EMPTY;
		} else if (rbTargetRandom.isSelected()) {
			selectedOption = TARGET_OPTION.RANDOM;
		} else if (rbTargetUseList.isSelected()) {
			selectedOption = TARGET_OPTION.USE_LIST;
		} else {
			throw new IllegalArgumentException("No valid target option selected!");
		}

		return selectedOption;
	}

	public Random getRandom(){
		if (random == null){
			if (seed == -1){
				random = new Random();
			} else {
				random = new Random(seed);
			}
		}
		return random;
	}

	public LinkedList<Integer> getTargetList() {
		return targetList;
	}

	public boolean showDialog(){
		return JOptionPane.showConfirmDialog(
				ProjectView.getMainWindow(),
				panel,
				"Create Random Pedestrians",
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}

	public boolean isValid() {
		return valid;
	}

}
