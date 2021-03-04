package org.vadere.state.traci;

public interface CompoundObjectProvider {

	default CompoundObject provide(){
		return provide(CompoundObjectBuilder.builder());
	};
	CompoundObject provide(CompoundObjectBuilder builder);

}
