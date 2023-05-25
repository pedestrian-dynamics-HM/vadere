package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;
import javax.swing.*;
import org.vadere.util.logging.Logger;

public class ActionCloseSettingDialog extends AbstractAction {
  private static Logger logger = Logger.getLogger(ActionCloseSettingDialog.class);
  private final JDialog dialog;

  public ActionCloseSettingDialog(final JDialog dialog) {
    this.dialog = dialog;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    dialog.setVisible(false);
  }
}
