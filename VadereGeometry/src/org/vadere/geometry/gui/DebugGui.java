package org.vadere.geometry.gui;

import org.vadere.geometry.gui.canvas.TriCanvas;
import org.vadere.geometry.mesh.inter.IFace;
import org.vadere.geometry.mesh.inter.IHalfEdge;
import org.vadere.geometry.mesh.inter.IVertex;
import org.vadere.geometry.shapes.IPoint;

import java.lang.reflect.InvocationTargetException;

import javax.swing.*;

/**
 * A Debug GUI which allows a step by step visualization of a triangulation algorithm. For creation
 * a {@link TriCanvas} is needed which is responsible to create the visualization for both java.swing
 * and, if needed, for tex tikz graphics. Both java.swing and tex/tikz visualizations can be decorated
 * with additional graphical information. Use the methods addGuiDecorator() and addTexDecorator() of
 * the {@link TriCanvas} element to add draw primitives for each visualization.
 *
 * The {@link DebugGui} can be controlled with menu actions an keyboard bindings.
 * <ul>
 * <li>Key: n "Next Step": continue with the algorithm and if the gui is called again show the new state.</li>
 * <li>Key: q "Stop Debugging Gui": dispose the gui, turn of the {@link DebugGui} and continue the algorithm.</li>
 * <li>Key: p "print Tikz output": create tex tikzi drawing and log to konsole</li>
 * <li>Key: s "Print State Info": print the State information defined within the {@link TriCanvas} implementation </li>
 * </ul>
 */
public class DebugGui<P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>> {

	private static DebugGui instance;
	private final Object o;

	//Displayed canvas. This will be set with at each  call to #showAndWait
	public TriCanvas<P, V, E, F> canvas;
	private JFrame frame;    //contains canvas
	private JMenuBar menuBar = new JMenuBar();    // Menu bar with supported actions.
	private boolean debugOn;
	private boolean initialized;

	private DebugGui() {
		o = new Object();
		debugOn = false;
	}

	//Statics

	/**
	 * @return Return Singletone instance of {@link DebugGui}
	 */
	public static DebugGui get() {
		if (instance == null) {
			instance = new DebugGui();
		}
		return instance;
	}

	/**
	 * Test if {@link DebugGui} is active.
	 *
	 * @return true if {@link DebugGui} will be shown if {@link #showAndWait(TriCanvas)} is called.
	 */
	public static boolean isDebugOn() {
		return get().debugOn;
	}

	/**
	 * Activate or Deactivate the {@link DebugGui}
	 */
	public static void setDebugOn(boolean debugOn) {
		get().debugOn = debugOn;
	}


	/**
	 * Activate {@link DebugGui}, show canvas and block here and wait for user input.
	 * This call will also active the {@link DebugGui} no matter what.
	 *
	 * @param canvas canvas to show
	 * @param <P>    P extends IPoint
	 * @param <V>    V extends IVertex<P>
	 * @param <E>    E extends IHalfEdge<P>
	 * @param <F>    F extends IFace<P>>
	 */
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>>
	void forceShowAndWait(TriCanvas<P, V, E, F> canvas) {
		setDebugOn(true);
		showAndWait(canvas);
	}

	/**
	 * If activated show canvas and block here and wait for user input.
	 *
	 * @param canvas canvas to show
	 * @param <P>    P extends IPoint
	 * @param <V>    V extends IVertex<P>
	 * @param <E>    E extends IHalfEdge<P>
	 * @param <F>    F extends IFace<P>>
	 */
	public static <P extends IPoint, V extends IVertex<P>, E extends IHalfEdge<P>, F extends IFace<P>>
	void showAndWait(TriCanvas<P, V, E, F> canvas) {
		if (isDebugOn()) {
			get().updateGui(canvas);
			get().waitForClick();
		}
	}

	// instance

	/**
	 * Add Menu and shortcut for actions
	 */
	private void initComponents() {
		Runnable runner = () -> {
			frame = new JFrame();
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

			JMenu menu = new JMenu("Edit");

			// Wake up waiting thread and continue with execution.
			JMenuItem nextStepItem = new JMenuItem("Next Step");
			menu.add(nextStepItem);
			nextStepItem.setAccelerator(KeyStroke.getKeyStroke('n'));
			nextStepItem.addActionListener(e -> {
				frame.setVisible(false);
				synchronized (o) {
					o.notifyAll();
				}
			});

			// Stop DebugGui and wake up waiting threads.
			JMenuItem stopDebugItem = new JMenuItem("Stop Debugging Gui");
			menu.add(stopDebugItem);
			stopDebugItem.setAccelerator(KeyStroke.getKeyStroke('q'));
			stopDebugItem.addActionListener(e -> {
				frame.setVisible(false);
				get().stopDebugging();
			});

			// Create Tikz drawing for current state of the mesh
			JMenuItem printTikzOutputItem = new JMenuItem("print Tikz output");
			menu.add(printTikzOutputItem);
			printTikzOutputItem.setAccelerator(KeyStroke.getKeyStroke('p'));
			printTikzOutputItem.addActionListener(e -> {
				System.out.println("%%%%%%%%%%%%");
				System.out.println("");
				canvas.getTexGraphBuilder().generateGraph();
				System.out.println(canvas.getTexGraphBuilder().returnString());
				System.out.println("");
				System.out.println("%%%%%%%%%%%%");
			});

			// Print State information provided via a StringBuilder consumer.
			JMenuItem printMeshItem = new JMenuItem("Print State Info");
			menu.add(printMeshItem);
			printMeshItem.setAccelerator(KeyStroke.getKeyStroke('s'));
			printMeshItem.addActionListener(e -> {
				StringBuilder sb = new StringBuilder();
				canvas.getStateLog().accept(sb);
				System.out.println(sb.toString());
			});

			menuBar.add(menu);
			frame.setJMenuBar(menuBar);
		};

		if(SwingUtilities.isEventDispatchThread()) {
			runner.run();
		}
		else {
			try {
				SwingUtilities.invokeAndWait(runner);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Remove old state from this instance and show the new one.
	 *
	 * @param canvas new {@link TriCanvas} implementation used to show current state.
	 */
	private void updateGui(TriCanvas canvas) {
		if(!initialized) {
			initComponents();
		}

		this.canvas = canvas;

		Runnable runner = () -> {
			frame.getContentPane().removeAll();
			frame.setSize((int) canvas.width, (int) canvas.height);
			frame.add(canvas);
			frame.setVisible(true);
			frame.revalidate();
			frame.repaint();
			if (canvas.logOnGuiUpdate() != null && canvas.logOnGuiUpdate().length() > 0)
				System.out.println(canvas.logOnGuiUpdate().toString());
		};

		if(SwingUtilities.isEventDispatchThread()) {
			runner.run();
		}
		else {
			try {
				SwingUtilities.invokeAndWait(runner);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Wait for input. A test for isDebugOn is not necessary the method is only called if
	 * isDebugOn is true.
	 */
	private void waitForClick() {
		if(!initialized) {
			initComponents();
		}

		synchronized (o) {
			try {
				o.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Deactivate Debugging, dispose frame and notify all waiting threads.
	 */
	private void stopDebugging() {
		if(!initialized) {
			initComponents();
		}

		setDebugOn(false);
		frame.dispose();
		frame = null;
		canvas = null;
		instance = null;
		System.out.println("notify");
		synchronized (o) {
			o.notifyAll();
		}
	}
}
