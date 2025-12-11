package org.vadere.state.types;

public enum EikonalSolverType {

	/**
	 * solvers on a regular grid
	 */
	FAST_MARCHING,
	HIGH_ACCURACY_FAST_MARCHING,
	FAST_ITERATIVE_METHOD,
	INFORMED_FAST_ITERATIVE_METHOD,  // Use this if you have many CPU cores

	/**
	 * Solvers on an triangle mesh
	 */
	FAST_MARCHING_TRI,
	FAST_ITERATIVE_METHOD_TRI, // Use this if you have many CPU cores

	/**
	 * No solver at all
	 */
	NONE;

	public boolean isHighAccuracy() {
		return this == HIGH_ACCURACY_FAST_MARCHING;
	}

	public boolean isUsingCellGrid() {
		return  this == FAST_MARCHING ||
				this == HIGH_ACCURACY_FAST_MARCHING ||
				this == FAST_ITERATIVE_METHOD ||
				this == INFORMED_FAST_ITERATIVE_METHOD;
	}
}
