package domain.model;

import helper.AbstractRatingSet;
import helper.SimilarElement;
import helper.UserRatingSet;

import java.util.Queue;

import domain.Rating;

public class UserCollaborativeFilteringModel extends AbstractCollaborativeFilteringModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5344448264711157886L;
	
	public UserCollaborativeFilteringModel(String trainingSetFile, int n) {
		super(trainingSetFile, n);
	}
	
	@Override
	public Rating predict(Rating r) {
		Queue<SimilarElement> simUsers = similarElements.get(r.getUserId());
		if (simUsers == null) {
			return r.reRate((float) 3.0);
		} else {
			Rating result = r.reRate((float)generateRatingFromSimilar(simUsers, trainSet, r.getUserId(), r.getMovieId()));
			System.out.println("result = " + result.getRating() + " userId = " + r.getUserId());
			return result;
		}
	}
	
	/*
	 * Abstract method implementations
	 */
	
	@Override
	protected AbstractRatingSet generateRatingSet() {
		return new UserRatingSet();
	}
}
