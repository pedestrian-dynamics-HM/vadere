package org.vadere.gui.postvisualization.control;

import com.fasterxml.jackson.databind.JsonNode;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.postvisualization.model.PedestrianColorTableModel;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.util.io.parser.JsonLogicParser;
import org.vadere.util.io.parser.VPredicate;
import org.vadere.util.logging.Logger;

import java.text.ParseException;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * @author Benedikt Zoennchen
 */
public class TableListenerLogicExpression implements TableModelListener {

	private static final Logger logger = Logger.getLogger(TableListenerLogicExpression.class);
	private PostvisualizationModel model;
	private PedestrianColorTableModel pedestrianColorTableModel;

	public TableListenerLogicExpression(@NotNull final PostvisualizationModel model, @NotNull final PedestrianColorTableModel pedestrianColorTableModel) {
		this.model = model;
		this.pedestrianColorTableModel = pedestrianColorTableModel;
	}

	@Override
	public void tableChanged(final TableModelEvent e) {
		for (int row = e.getFirstRow(); row <= e.getLastRow(); row++) {
			if (row >= 0 && e.getColumn() == PedestrianColorTableModel.CIRTERIA_COLUMN) {
				try {
					String expression = pedestrianColorTableModel.getValueAt(row, e.getColumn()).toString();
					VPredicate<JsonNode> evaluator = new JsonLogicParser(expression).parse();
					model.putExpression(row, evaluator);
				} catch (ParseException e1) {
					model.removeExpression(row);
					pedestrianColorTableModel.setValueAt("", e.getColumn(), row);
					logger.warn(e1.getLocalizedMessage());
				}
			}
		}
	}
}
