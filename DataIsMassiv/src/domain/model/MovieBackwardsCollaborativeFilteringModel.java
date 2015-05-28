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
	
	/**
	 * flips rating to other side of the rating scale, centered around the avg. Truncates scale at 1.0 and 5.0
	 */
	@Override
	protected double getFlippedRatingIfNecessary(double result, double avg) {
		result = 2 * avg - result;
		if (result > 5.0) {
			result = 5.0;
		} else if (result < 1.0) {
			result = 1.0;
		}
		return result;
	}
	
	@Override
	protected double getFlipedRatingIfNeccesary(double sim) {
		return -1 * sim;
	}
	
	@Override
	protected String getLogFileName() {
		return "BMCF.log";
	}
}
