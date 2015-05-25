package domain.model;

import helper.AbstractRatingSet;
import helper.MovieRatingSet;

public class MovieCollaborativeFilteringModel extends AbstractCollaborativeFilteringModel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -555873276166650438L;

	public MovieCollaborativeFilteringModel(String trainingSetFile, int n) {
		super(trainingSetFile, n);
	}
	
	/*
	 * Abstract method implementations
	 */
	
	@Override
	protected AbstractRatingSet generateRatingSet() {
		return new MovieRatingSet();
	}
}
