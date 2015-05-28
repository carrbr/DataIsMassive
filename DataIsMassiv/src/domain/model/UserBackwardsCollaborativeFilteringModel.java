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
	
	@Override
	protected double getFlippedRatingIfNecessary(double result) {
		return 6 - result; // gets rating on inverted scale, i.e. 1.1 is now 6 - 1.1 = 4.9
	}
	
	@Override
	protected double getFlipedRatingIfNeccesary(double sim) {
		return -1 * sim;
	}
}
