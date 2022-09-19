package org.vadere.state.attributes.distributions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.distribution.impl.SingleSpawnDistribution;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        defaultImpl = Void.class
        )
@JsonSubTypes({
        @JsonSubTypes.Type(value = AttributesBinomialDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesBinomialDistribution"),
        @JsonSubTypes.Type(value = AttributesConstantDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesConstantDistribution"),
        @JsonSubTypes.Type(value = AttributesEmpiricalDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesEmpiricalDistribution"),
        @JsonSubTypes.Type(value = AttributesLinearInterpolationDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesLinearInterpolationDistribution"),
        @JsonSubTypes.Type(value = AttributesMixedDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesMixedDistribution"),
        @JsonSubTypes.Type(value = AttributesNegativeExponentialDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesNegativeExponentialDistribution"),
        @JsonSubTypes.Type(value = AttributesNormalDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesNormalDistribution"),
        @JsonSubTypes.Type(value = AttributesPoissonDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesPoissonDistribution"),
        @JsonSubTypes.Type(value = SingleSpawnDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesSingleSpawnDistribution"),
        @JsonSubTypes.Type(value = AttributesTimeSeriesDistribution.class, name = "org.vadere.state.attributes.distributions.AttributesTimeSeriesDistribution"),
})
public abstract class AttributesDistribution extends Attributes {
}
