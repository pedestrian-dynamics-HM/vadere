package org.vadere.gui.topographycreator.view;

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
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

public class ActionRandomPedestrianDialog {

	public enum TARGET_OPTION { EMPTY, RANDOM, USE_LIST }

	// Member Variables
	private JTextField firstPedIdField;
	private JTextField numberOfPedsField;
	private JTextField boundaryRectangleField;
	private JTextField targetsField;
	private JTextField seedField;
	private JTextField groupMembershipRatioField;

	private JRadioButton rbTargetEmpty;
	private JRadioButton rbTargetRandom;
	private JRadioButton rbTargetUseList;

	private JPanel panelWindow;
	private JPanel panelRadioButtons;

	private boolean valid;
	private int firstPedId;
	private int numOfPeds;
	private Rectangle2D.Double boundaryRectangle;
	private LinkedList<Integer> targetList;
	private int seed;
	private double groupMembershipRatio;
	private Random random;

	// Constructors
	public ActionRandomPedestrianDialog() {

		valid = false;
		firstPedId = 1;
		numOfPeds = 10;
		boundaryRectangle = new Rectangle2D.Double();
		targetList = new LinkedList<>();
		targetList.add(-1);
		seed = -1;
		groupMembershipRatio = 0.5;

		createGuiElements();
		groupGuiElements();
		placeGuiElements();

	}

	private void createGuiElements() {

		firstPedIdField = new JTextField(String.format(Locale.US, "%d", firstPedId), 15);
		firstPedIdField.setHorizontalAlignment(JTextField.RIGHT);
		firstPedIdField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				String text = firstPedIdField.getText();
				try {
					firstPedId = Integer.parseInt(text);
					setValid(true, firstPedIdField);
				} catch (Exception ex){
					setValid(false, firstPedIdField);
				}
			}
		});

		numberOfPedsField = new JTextField(String.format(Locale.US, "%d", numOfPeds), 15);
		numberOfPedsField.setHorizontalAlignment(JTextField.RIGHT);
		numberOfPedsField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				String text = numberOfPedsField.getText();
				try {
					numOfPeds = Integer.parseInt(text);
					setValid(true, numberOfPedsField);
				} catch (Exception ex){
					setValid(false, numberOfPedsField);
				}
			}
		});

		boundaryRectangleField = new JTextField("x, y, width, height", 15);
		boundaryRectangleField.setHorizontalAlignment(JTextField.RIGHT);
		boundaryRectangleField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				String text = boundaryRectangleField.getText();
				try {
					String[] numbersAsString = text.split(",");

					boundaryRectangle = new Rectangle2D.Double(
							Double.parseDouble(numbersAsString[0]),
							Double.parseDouble(numbersAsString[1]),
							Double.parseDouble(numbersAsString[2]),
							Double.parseDouble(numbersAsString[3])
							);

					setValid(true, boundaryRectangleField);
				} catch (Exception ex){
					setValid(false, boundaryRectangleField);
				}
			}
		});

		targetsField = new JTextField("-1", 15);
		targetsField.setHorizontalAlignment(JTextField.RIGHT);
		targetsField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				String strippedText = targetsField.getText().replace(" ", "");
				String[] splittedText = strippedText.split(",");

				try {
					targetList = Arrays.stream(splittedText).mapToInt(Integer::parseInt).boxed().collect(Collectors.toCollection(LinkedList::new));
					setValid(true, targetsField);
				}catch (Exception ex){
					setValid(false, targetsField);
				}
			}
		});

		createTargetRadioButtons();

		seedField = new JTextField(String.format(Locale.US, "%d", seed), 15);
		seedField.setHorizontalAlignment(JTextField.RIGHT);
		seedField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				try{
					seed = Integer.parseInt(seedField.getText());
					setValid(true, seedField);
				} catch (Exception ex){
					setValid(false, seedField);
				}
			}
		});

		groupMembershipRatioField = new JTextField(String.format(Locale.US, "%.1f", groupMembershipRatio), 15);
		groupMembershipRatioField.setHorizontalAlignment(JTextField.RIGHT);
		groupMembershipRatioField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void handle(DocumentEvent e) {
				try {
					groupMembershipRatio = Double.parseDouble(groupMembershipRatioField.getText());

					boolean isValid = (groupMembershipRatio >= 0.0 && groupMembershipRatio <= 1.0);
					setValid(isValid, groupMembershipRatioField);
				} catch (Exception ex) {
					setValid(false, groupMembershipRatioField);
				}
			}
		});

		panelWindow = new JPanel();
		panelRadioButtons = new JPanel();
	}

	private void createTargetRadioButtons() {

		rbTargetEmpty = new JRadioButton(Messages.getString("TopographyCreator.PlaceRandomPedestrians.targetEmptyOption.label"), true);
		rbTargetRandom = new JRadioButton(Messages.getString("TopographyCreator.PlaceRandomPedestrians.targetRandomOption.label"), false);
		rbTargetUseList = new JRadioButton(Messages.getString("TopographyCreator.PlaceRandomPedestrians.targetListOption.label"), false);
		targetsField.setEditable(false);

		ButtonGroup buttonGroupTarget = new ButtonGroup();
		buttonGroupTarget.add(rbTargetEmpty);
		buttonGroupTarget.add(rbTargetRandom);
		buttonGroupTarget.add(rbTargetUseList);

		rbTargetUseList.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean targetsFieldState = e.getStateChange() == ItemEvent.SELECTED;
				targetsField.setEditable(targetsFieldState);
			}
		});

	}

	private void groupGuiElements() {
		panelRadioButtons.setLayout(new FlowLayout());

		panelRadioButtons.add(rbTargetEmpty);
		panelRadioButtons.add(rbTargetRandom);
		panelRadioButtons.add(rbTargetUseList);
	}

	private void placeGuiElements() {
		panelWindow.setLayout(new GridBagLayout());
		int row = 0;
		int col0 = 0;
		int col1 = 1;

		panelWindow.add(new JLabel("First Pedestrian Id"), c(GridBagConstraints.HORIZONTAL, col0, row));
		panelWindow.add(firstPedIdField, c(GridBagConstraints.HORIZONTAL, col1, row++));

		panelWindow.add(new JLabel("Set Number of Pedestrians"), c(GridBagConstraints.HORIZONTAL, col0, row));
		panelWindow.add(numberOfPedsField, c(GridBagConstraints.HORIZONTAL, col1, row++));

		panelWindow.add(new JLabel("In Boundary Rectangle"), c(GridBagConstraints.HORIZONTAL, col0, row));
		panelWindow.add(boundaryRectangleField, c(GridBagConstraints.HORIZONTAL, col1, row++));

		panelWindow.add(new JLabel("Set Targets"), c(GridBagConstraints.HORIZONTAL, col0, row));
		panelWindow.add(panelRadioButtons, c(GridBagConstraints.HORIZONTAL, col1, row++));
		panelWindow.add(targetsField, c(GridBagConstraints.HORIZONTAL, col1, row++));

		panelWindow.add(new JLabel("Set Random Seed (-1 for random)"), c(GridBagConstraints.HORIZONTAL, col0, row));
		panelWindow.add(seedField, c(GridBagConstraints.HORIZONTAL, col1, row++));

		panelWindow.add(new JLabel("Ingroup Membership Ratio"), c(GridBagConstraints.HORIZONTAL, col0, row));
		panelWindow.add(groupMembershipRatioField, c(GridBagConstraints.HORIZONTAL, col1, row++));

		panelWindow.add(new JLabel("May take a while because intelligent distance function not available yet..."),
				c(GridBagConstraints.HORIZONTAL, col0,row,2));
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

	// Getter
	public boolean isValid() {
		return valid;
	}

	public int getFirstPedId() {
		return firstPedId;
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

	public LinkedList<Integer> getTargetList() {
		return targetList;
	}

	public double getGroupMembershipRatio() {
		return groupMembershipRatio;
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

	// Setter
	private void setValid(boolean valid, JComponent causingElement) {
		this.valid = valid;

		Color color = (valid) ? Color.BLACK : Color.RED;
		causingElement.setForeground(color);
	}

	// Methods
	public boolean showDialog() {
		int returnValue = JOptionPane.showConfirmDialog(
				ProjectView.getMainWindow(),
				panelWindow,
				Messages.getString("TopographyCreator.PlaceRandomPedestrians.label"),
				JOptionPane.OK_CANCEL_OPTION);

		return returnValue == JOptionPane.OK_OPTION;
	}

}
