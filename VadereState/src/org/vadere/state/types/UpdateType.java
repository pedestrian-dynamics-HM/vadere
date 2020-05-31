package org.vadere.state.types;

public enum UpdateType {
	SEQUENTIAL, EVENT_DRIVEN, PARALLEL, SHUFFLE,
	PARALLEL_CL_CPU, PARALLEL_CL_GPU,
	PARALLEL_SHARED_MEM_CL_CPU, PARALLEL_SHARED_MEM_CL_GPU,
	EVENT_DRIVEN_CL_CPU, EVENT_DRIVEN_CL_GPU,
	EVENT_DRIVEN_PARALLEL;

	public boolean isGPUScheme() {
		switch (this) {
			case PARALLEL_SHARED_MEM_CL_GPU:
			case EVENT_DRIVEN_CL_GPU:
			case PARALLEL_CL_GPU: return true;
			default:return false;
		}
	}
}
