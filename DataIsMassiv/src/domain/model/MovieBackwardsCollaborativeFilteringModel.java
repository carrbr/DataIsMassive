package domain.model;

import helper.AbstractRatingSet;
import helper.MovieRatingSet;

public class MovieBackwardsCollaborativeFilteringModel extends AbstractCollaborativeFilteringModel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -555873276166650438L;

	public MovieBackwardsCollaborativeFilteringModel(String trainingSetFile, int n, double minSim, int minCount, double pcWeight) {
		super(trainingSetFile, n, minSim, minCount, pcWeight);
	}
	
	/*
	 * Abstract method implementations
	 */
	
	@Override
	protected AbstractRatingSet generateRatingSet() {
		return new MovieRatingSet();
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
