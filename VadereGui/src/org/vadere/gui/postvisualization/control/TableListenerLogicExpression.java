package org.vadere.gui.postvisualization.control;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.postvisualization.model.PredicateColoringModel;

/** @author Benedikt Zoennchen */
public class TableListenerLogicExpression implements TableModelListener {

  private PredicateColoringModel model;

  public TableListenerLogicExpression(@NotNull final PredicateColoringModel model) {
    this.model = model;
  }

  @Override
  public void tableChanged(final TableModelEvent e) {
    model.update(e);
  }
}
