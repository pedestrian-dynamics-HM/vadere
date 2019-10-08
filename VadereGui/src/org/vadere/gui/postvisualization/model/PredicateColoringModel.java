package org.vadere.gui.postvisualization.model;

import com.fasterxml.jackson.databind.JsonNode;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.postvisualization.control.TableListenerLogicExpression;
import org.vadere.state.scenario.Agent;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.parser.JsonLogicParser;
import org.vadere.util.io.parser.VPredicate;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.event.TableModelEvent;

/**
 * This class is the model in the sense of the CVM pattern of the GUI which
 * saves all {@link VPredicate} which can be defined to color agents e.g. the
 * user can fill the {@link PedestrianColorTableModel} with expressions like
 * agent.position.x > 4 and define a color such as red for this expression. Therefore
 * this class will return red for an agent with a x-coordinate > 4. This class
 * implements the logic to add and remove such predicates while the
 * {@link PedestrianColorTableModel} does only hold the data.
 *
 * @author Benedikt Zoennchen
 */
public class PredicateColoringModel {
	private static final Logger logger = Logger.getLogger(PredicateColoringModel.class);

	private static final VPredicate<JsonNode> FALSE = pred -> false;

	// TODO: colorEvalFunctions seem to be unnecessary.
	/**
	 * Holds the actual data, i.e. all predicates in text form.
	 */
	private final PedestrianColorTableModel pedestrianColorTableModel;

	/**
	 * Holds the acutal date, i.e. all predicates as {@link VPredicate}.
	 */
	private final Map<Integer, VPredicate<JsonNode>> colorEvalFunctions;

	public PredicateColoringModel() {
		this.colorEvalFunctions = new HashMap<>();
		this.pedestrianColorTableModel = new PedestrianColorTableModel();
		this.pedestrianColorTableModel.addTableModelListener(new TableListenerLogicExpression(this));
	}

	/**
	 * Returns the color of an agent defined by a set of predicates. This set
	 * can be manipulated by {@link #putExpression(int, VPredicate)} and
	 * {@link #removeExpression(int)};
	 *
	 * @param agent an agent of this model.
	 *
	 * @return a color
	 */
	public Optional<Color> getColorByPredicate(final Agent agent) {
		JsonNode jsonObj = StateJsonConverter.toJsonNode(agent);
		Optional<Map.Entry<Integer, VPredicate<JsonNode>>> firstEntry = colorEvalFunctions.entrySet()
				.stream()
				.filter(entry -> parseIgnoreException(entry.getValue(), jsonObj))
				.findFirst();

		if (firstEntry.isPresent()) {
			return Optional.of((Color) pedestrianColorTableModel.getValueAt(firstEntry.get().getKey(),
					PedestrianColorTableModel.COLOR_COLUMN));
		}

		return Optional.empty();
	}

	/**
	 * Inserts a new predicate, i.e. logical expression defined in json format.
	 *
	 * @param row       the row at which the predicate will be inserted
	 * @param predicate the predicate
	 */
	private void putExpression(final int row, @NotNull final VPredicate<JsonNode> predicate) {
		colorEvalFunctions.put(row, predicate);
	}

	/**
	 * removes the predicate at a certain row.
	 * @param row the row
	 */
	private void removeExpression(final int row) {
		colorEvalFunctions.remove(row);
	}

	/**
	 * Returns the actual model which contains all the data.
	 *
	 * @return the actual model which contains all the data
	 */
	public PedestrianColorTableModel getPedestrianColorTableModel() {
		return pedestrianColorTableModel;
	}

	/**
	 * This will be called if the user edits the acutal swing {@link javax.swing.JTable}.
	 * The content of the table will be update if and only if the predicate, which was inserted
	 * by the user into a row of the table, is syntactically valid.
	 *
	 * @param e the event of the swing table
	 */
	public void update(final TableModelEvent e) {
		for (int row = e.getFirstRow(); row <= e.getLastRow(); row++) {
			if (row >= 0 && e.getColumn() == PedestrianColorTableModel.CIRTERIA_COLUMN) {
				try {
					String expression = pedestrianColorTableModel.getValueAt(row, e.getColumn()).toString();
					VPredicate<JsonNode> evaluator = new JsonLogicParser(expression).parse();
					putExpression(row, evaluator);
				} catch (ParseException e1) {
					//removeExpression(row);
					//pedestrianColorTableModel.setValueAt("", e.getColumn(), row);
					putExpression(row, FALSE);
					logger.warn(e1.getLocalizedMessage());
				}
			}
		}
	}

	/**
	 * Returns true if the agent (defined by the {@link JsonNode} <tt>node</tt>) fulfills the predicate.
	 *
	 * @param predicate the predicate
	 * @param node      the agent in json format.
	 *
	 * @return true if the agent fulfills the predicate
	 */
	private boolean parseIgnoreException(@NotNull final VPredicate<JsonNode> predicate, @NotNull final JsonNode node) {
		try {
			return predicate.test(node);
		} catch (ParseException e) {
			return false;
		}
	}
}
