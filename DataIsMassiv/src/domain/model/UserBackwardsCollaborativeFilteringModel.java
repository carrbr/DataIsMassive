package domain.model;

import helper.AbstractRatingSet;
import helper.UserRatingSet;

public class UserBackwardsCollaborativeFilteringModel extends AbstractCollaborativeFilteringModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5344448264711157886L;
	
	public UserBackwardsCollaborativeFilteringModel(String trainingSetFile, int n, double minSim, int minCount, double pcWeight) {
		super(trainingSetFile, n, minSim, minCount, pcWeight);
	}
	
	/*
	 * Abstract method implementations
	 */
	
	@Override
	protected AbstractRatingSet generateRatingSet() {
		return new UserRatingSet();
	}
	
	/**
	 * flips rating to other side of the rating scale, centered around the avg. Truncates scale at 1.0 and 5.0
	 */
	@Override
	protected double getFlippedRatingIfNecessary(double result, double avg) {
		result = 2 * avg - result;
		return result;
	}
	
	@Override
	protected double getFlipedRatingIfNeccesary(double sim) {
		return -1 * sim;
	}
	
	@Override
	protected String getLogFileName() {
		return "BUCF.log";
	}
}
