package org.vadere.gui.components.view;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.util.LayoutStyle;

/**
 * Currently UNUSED!
 * 
 * A Dialog that can contain multiple Actions.
 * 
 * 
 */
public class ActionDialog extends JDialog {

	private static final long serialVersionUID = 1860465611717157530L;
	private static ActionDialog instance = null;
	// private final Component parent;
	private List<Action> actions;
	private JToolBar toolbar;

	/**
	 * Factory-Method of this Dialog. Create's a new ActionDialog.
	 * 
	 * @param parent the parent Component of new ActionDialog
	 * @return a new or restored ActionDialog
	 * @throws IOException
	 */
	public static ActionDialog createPaintMethodDialog(final Component parent) throws IOException {
		if (instance == null) {
			instance = new ActionDialog(parent);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				instance.setVisible(true);
				// focus the parent jframe!
				SwingUtilities.getWindowAncestor(parent).requestFocus();
			}
		});
		return instance;
	}

	public static void hideDialog() {
		if (instance != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					instance.setVisible(false);
				}
			});

		}
	}

	public void refresh() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				toolbar.removeAll();
				for (Action action : actions) {
					JButton button = toolbar.add(action);
					button.setBorderPainted(false);
					button.setAlignmentX(CENTER_ALIGNMENT);
				}
				pack();
				repaint();
			};
		});
	}

	private ActionDialog(final Component parent) throws IOException {
		super(SwingUtilities.getWindowAncestor(parent));
		this.actions = new ArrayList<Action>();

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				toolbar = new JToolBar(SwingConstants.VERTICAL);
				getContentPane().setLayout(
						new FormLayout(new ColumnSpec[] {ColumnSpec.decode("left:2dlu"), ColumnSpec.decode("pref:grow"),
								ColumnSpec.decode("left:2dlu"),}, new RowSpec[] {
										RowSpec.createGap(LayoutStyle.getCurrent().getNarrowLinePad()),
										RowSpec.decode("fill:pref:grow"),
										RowSpec.createGap(LayoutStyle.getCurrent().getNarrowLinePad())}));

				toolbar.setAlignmentX(CENTER_ALIGNMENT);
				toolbar.setFloatable(false);

				getContentPane().add(toolbar, "2, 2");

				setDefaultCloseOperation(HIDE_ON_CLOSE);
				pack();
				setResizable(false);
				// very useful! JFrame does not lose the focus anymore!
				setAutoRequestFocus(false);
				setAlwaysOnTop(true);
				setVisible(true);
				setLocation(Math.max(0, parent.getX() - getWidth()), Math.max(0, parent.getY()));
			}
		});

	}

	public void addAction(final Action action) {
		actions.add(action);
	}

	public void addAllActions(final List<Action> actions) {
		this.actions.addAll(actions);
	}

	public void removeAllActions() {
		actions.clear();
	}

	public void removeAction(final Action action) {
		actions.remove(action);
	}
}
