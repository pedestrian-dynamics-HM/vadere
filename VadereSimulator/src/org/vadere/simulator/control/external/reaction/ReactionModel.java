package org.vadere.simulator.control.external.reaction;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.util.math.TruncatedNormalDistribution;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class ReactionModel implements IReactModel{

    private boolean isReactingToRecurringInformation;
    private boolean isReactingToFirstInformationOnly;

    private int numberOfReactionBehaviors;
    private HashMap<Integer, DistParameters> distParameters = new HashMap<>();

    private final Random random = new Random(0);
    private final JDKRandomGenerator randomGenerator = new JDKRandomGenerator(random.nextInt());


    public ReactionModel(String commandStr){
        setParameters(commandStr);
    }

    public void setParameters(String commandStr) {
        ReactionParameter reactionParameter = new ReactionParameter(commandStr);
        numberOfReactionBehaviors = reactionParameter.getNrOptions();
        distParameters = reactionParameter.getDist();
        isReactingToRecurringInformation = reactionParameter.isReactingToRecurringInformation();
        isReactingToFirstInformationOnly = reactionParameter.isReactingToFirstInformationOnly();
    }

    public ReactionModel() {
        numberOfReactionBehaviors = 1;
        distParameters.put(0,new DistParameters());
    }

    public ReactionModel(double bernoulliParameter) {
        numberOfReactionBehaviors = 1;
        distParameters.put(0,new DistParameters(bernoulliParameter));
    }



    public boolean isPedReact(int i) throws Exception{

        double bernoulliParameter = getBernoulliParameter(i);
        return (new BinomialDistribution(randomGenerator, 1, bernoulliParameter).sample() == 1.0);
    }

    public boolean isPedReact() throws Exception{
        if (numberOfReactionBehaviors > 1){
            throw new Exception("There are " + numberOfReactionBehaviors + " reaction behavior probabilities defined. Please choose an option.");
        }

        return isPedReact(0);
    }

    private double getBernoulliParameter(int i){

        if (numberOfReactionBehaviors == 1){
            i = 0;
        }

        DistParameters p = distParameters.get(i);
        LinkedList<Double> args = p.getDistributionParameters();
        String name = p.getDistType();
        double bernoulliP;

        if (name.equalsIgnoreCase(BernoulliParameterDistributions.GAUSSIAN.toString())){
            bernoulliP =  new TruncatedNormalDistribution(randomGenerator, args.get(0), args.get(1), 0, 1, 100).sample();
        }
        else if (name.equalsIgnoreCase(BernoulliParameterDistributions.UNIFORM.toString())){
            bernoulliP =  new UniformRealDistribution(randomGenerator, args.get(0), args.get(1)).sample();
        }
        else if (name.equalsIgnoreCase(BernoulliParameterDistributions.FIXED.toString())){
            bernoulliP = args.get(0);
        }
        else{
            throw new IllegalArgumentException("Got distribution type " + name + ". Allowed: Gaussian, Uniform, None.");
        }

        return bernoulliP;


    }


    public boolean isReactingToFirstInformationOnly() {
        return isReactingToFirstInformationOnly;
    }

    public boolean isReactingToRecurringInformation() {
        return isReactingToRecurringInformation;
    }
}
