package helper;

import domain.Rating;

/**
 * 
 * @author BrianCarr
 *
 *         This class is a set of ratings organized by user, designed to allow
 *         O(1) time additions of user ratings, as well as to allow easy
 *         iteration through all available user ratings
 */
public class UserRatingSet extends AbstractRatingSet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1049867905208470615L;

	public UserRatingSet() {
		super();
	}

	/*
	 * Implementations of abstract methods
	 */

	@Override
	public int getFilterByIdFromRating(Rating r) {
		return r.getUserId();
	}

	@Override
	public int getFeatureIdFromRating(Rating r) {
		return r.getMovieId();
	}
}
