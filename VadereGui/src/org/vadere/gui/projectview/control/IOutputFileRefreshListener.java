package org.vadere.gui.projectview.control;

/**
 * Since the refresh of the output files will be done in a separate thread (to avoid gui locks),
 * this listener will be notified if the the refresh starts and ends.
 *
 */
public interface IOutputFileRefreshListener {

	/**
	 * signal that the refresh will start very soon.
	 */
	void preRefresh();

	/**
	 * signal that the refresh ended.
	 */
	void postRefresh();

}
