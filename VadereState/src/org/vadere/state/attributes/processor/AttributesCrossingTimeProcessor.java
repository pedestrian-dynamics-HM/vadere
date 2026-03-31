package org.vadere.state.attributes.processor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Benedikt Zoennchen
 */
@JsonIgnoreProperties({ "waitingAreaId" }) // waitingAreaId is a legacy attribute, and not used anymore
public class AttributesCrossingTimeProcessor extends AttributesAreaProcessor {
}
