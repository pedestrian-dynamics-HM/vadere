package org.vadere.simulator.control.external.reaction;

public enum BernoulliParameterDistributions {
    FIXED("none"),
    GAUSSIAN("NormalDistribution"),
    UNIFORM("UniformRealDistribution");

    String distname;

    BernoulliParameterDistributions(String distname){
        this.distname = distname;
    }

    public String getDistname(BernoulliParameterDistributions dist){
        return this.distname;
    }


    public String toString() {
        switch (this) {
            case FIXED:
                return "none";
            case GAUSSIAN:
                return "Gaussian";
            case UNIFORM:
                return "Uniform";
            default:
                return this.toString();
        }
    }
}
