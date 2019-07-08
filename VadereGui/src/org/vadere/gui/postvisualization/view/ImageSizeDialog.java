package org.vadere.gui.postvisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.openide.NotifyDescriptor;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.components.view.SettingsDialog;
import org.vadere.gui.postvisualization.control.ActionCloseSettingDialog;
import org.vadere.gui.postvisualization.utils.ImageGenerator;
import org.vadere.util.logging.Logger;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import javax.swing.*;

public class ImageSizeDialog extends JDialog {
	private static Logger logger = Logger.getLogger(SettingsDialog.class);
	private static Resources resources = Resources.getInstance("postvisualization");
	private double scale;
	private SimulationModel<? extends DefaultSimulationConfig> model;
	private final SpinnerNumberModel sModelImageWidth;
	private final SpinnerNumberModel sModelImageHeight;
	private Rectangle2D.Double imageSize;
	private State state = State.Cancle;

	public ImageSizeDialog(final SimulationModel<? extends DefaultSimulationConfig> model) {

		this.scale = 1.0;
		this.model = model;
		this.imageSize = new Rectangle2D.Double(0, 0, ImageGenerator.calculateOptimalWidth(model),
				ImageGenerator.calculateOptimalHeight(model));
		this.setTitle(Messages.getString("ImageSizeDialog.title"));
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.addWindowListener(
				new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						new ActionCloseSettingDialog(ImageSizeDialog.this).actionPerformed(null);
					}
				});

		// ################################# Build main structure
		// #############################
		FormLayout mainLayout = new FormLayout("5dlu, pref, 2dlu, pref, 2dlu, pref, 5dlu", // 2 col
				"5dlu, pref, 2dlu, pref, 2dlu, pref, 5dlu"); // rows
		CellConstraints cc = new CellConstraints();

		getContentPane().setLayout(mainLayout);

		JLabel lblWidth = new JLabel(Messages.getString("ImageSizeDialog.lblWidth.text") + ":");
		final JSpinner spinnerWidth = new JSpinner();
		sModelImageWidth = new SpinnerNumberModel(ImageGenerator.calculateOptimalWidth(model), 1, 5000, 1);
		spinnerWidth.setModel(sModelImageWidth);
		lblWidth.setLabelFor(spinnerWidth);

		getContentPane().add(lblWidth, cc.xy(2, 2));
		getContentPane().add(spinnerWidth, cc.xy(4, 2));
		getContentPane().add(new JLabel("px"), cc.xy(6, 2));


		JLabel lblHeight = new JLabel(Messages.getString("ImageSizeDialog.lblHeight.text") + ":");
		final JSpinner spinnerHeight = new JSpinner();
		sModelImageHeight = new SpinnerNumberModel(ImageGenerator.calculateOptimalHeight(model), 1, 5000, 1);
		spinnerHeight.setModel(sModelImageHeight);

		getContentPane().add(lblHeight, cc.xy(2, 4));
		getContentPane().add(spinnerHeight, cc.xy(4, 4));
		getContentPane().add(new JLabel("px"), cc.xy(6, 4));

		spinnerWidth.addChangeListener((e) -> {
			AffineTransform at = new AffineTransform();
			double value = sModelImageWidth.getNumber().doubleValue();
			double scale = sModelImageWidth.getNumber().doubleValue() / imageSize.getWidth();
			boolean increase = scale <= 1.0;

			at.scale(scale, scale);
			imageSize = (Rectangle2D.Double) at.createTransformedShape(imageSize).getBounds2D();

			int height;
			if (increase) {
				height = (int) Math.ceil(imageSize.getHeight());
			} else {
				height = (int) Math.floor(imageSize.getHeight());
			}
			spinnerHeight.setValue(height);
		});

		spinnerHeight.addChangeListener((e) -> {
			AffineTransform at = new AffineTransform();
			double value = sModelImageHeight.getNumber().doubleValue();
			double scale = value / imageSize.getHeight();
			boolean increase = scale <= 1.0;

			at.scale(scale, scale);
			imageSize = (Rectangle2D.Double) at.createTransformedShape(imageSize).getBounds2D();

			int width;
			if (increase) {
				width = (int) Math.ceil(imageSize.getWidth());
			} else {
				width = (int) Math.floor(imageSize.getWidth());
			}
			spinnerWidth.setValue(width);
		});

		JButton btOk = new JButton(Messages.getString("ProjectView.btnOk"));
		JButton btCancel = new JButton(Messages.getString("ProjectView.btnCancel"));

		btCancel.addActionListener((e) -> {
			state = State.Cancle;
			ImageSizeDialog.this.dispose();
		});

		btOk.addActionListener((e) -> {
			state = State.Ok;
			ImageSizeDialog.this.dispose();
		});

		getContentPane().add(btCancel, cc.xy(2, 6));
		getContentPane().add(btOk, cc.xy(4, 6));

		setModal(true);
		setAlwaysOnTop(true);

		pack();
		setResizable(false);
		SwingUtils.centerComponent(this);
		setVisible(true);
	}

	public Rectangle2D.Double getImageBound() {
		return imageSize;
	}

	public double getScaleFactor() {
		return imageSize.getWidth() / model.getWindowBound().getWidth();
	}

	public State getState() {
		return state;
	}

	public enum State {
		Cancle, Ok;
	}
}
