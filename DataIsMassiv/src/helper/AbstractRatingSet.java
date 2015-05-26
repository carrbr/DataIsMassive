package helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import be.tarsos.lsh.Vector;
import domain.Rating;

/**
 * 
 * @author BrianCarr
 *
 * This class is a set of ratings organized by user, designed to allow O(1) 
 * time additions of user ratings, as well as to allow easy iteration through
 * all available user ratings
 */
public abstract class AbstractRatingSet implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3370186565165421203L;
	/*
	 * The Integer key is the userID
	 * 
	 * The ArrayList of Ratings contains all ratings for that user
	 */
	protected Map<Integer, ArrayList<Rating>> ratings;
	protected int maxFeatureId;
	protected int maxFilterById;
	protected Map<Integer, Double> filterByElemMeans;
	
	public AbstractRatingSet() {
		ratings = new Hashtable<Integer, ArrayList<Rating>>();
		maxFeatureId = 0;
		maxFilterById = 0;
	}
	
	public void addFilterByElemRating(Rating r) {
		int filterById = getFilterByIdFromRating(r);
		int featureId = getFeatureIdFromRating(r);

		if (ratings.containsKey(filterById)) {
			// filterById already exists in table, simply add rating to their vector
			ratings.get(filterById).add(r);
		} else { // filterById does not yet exist, add them
			ArrayList<Rating> ratingVector = new ArrayList<Rating>();
			ratingVector.add(r);
			ratings.put(filterById, ratingVector);
		}
		
		// book keeping for IDs
		if (featureId > this.maxFeatureId) {
			this.maxFeatureId = featureId;
		}
		
		if (filterById > this.maxFilterById) {
			this.maxFilterById = filterById;
		}
	}
	
	public Vector getFilterByElemRatingsAsNormedVector(int filterById) {
		Vector ratingVec = new Vector(maxFeatureId + 1); // vector should have same dimensionality as the number of movies
		ratingVec.setKey(Integer.toString(filterById));

		if (!ratings.containsKey(filterById)) {
			return null;
		}
		
		double avg = this.getMeanForFilterById(filterById);
		
		Rating r = ratings.get(filterById).get(0);
		int featureId = getFeatureIdFromRating(r);
		for (int i = 0; i <= maxFeatureId; i++) {
			if (i == featureId) { // it is now this ratings index
				ratingVec.set(i, r.getRating() - avg);
				if (i + 1 < ratings.get(filterById).size() && i + 1 <= maxFeatureId) {
					r = ratings.get(filterById).get(i + 1); // prepare next rating
					featureId = getFeatureIdFromRating(r);
				}
			} else { // empty element in sparse rating data -- fill it in
				ratingVec.set(i, 0.0);

			}
		}
		ratingVec.setKey(Integer.toString(filterById));
		
		return ratingVec;
	}
	
	public ArrayList<Rating> getFilterByElemRatings(int filterById) {
		return ratings.get(filterById); // TODO this might return null... is that OK?
	}
	
	public float getRatingValue(int filterById, int featureId) {
		ArrayList<Rating> ratingList = ratings.get(filterById);
		if (ratingList == null) { // this rating does not exist
			return (float) 0.0; // TODO this is questionable
		}
		// find the correct rating
		for (Rating rating: ratingList) {
			if (getFeatureIdFromRating(rating) == featureId) {
				return rating.getRating();
			}
		}
		// filterById didn't rate this featureId
		return 0; // TODO questionable decision, may have to change this later
	}
	
	public double getMeanForFilterById(int filterById) {
		double avg = 0.0;
		if (this.filterByElemMeans.containsKey(filterById)) { // avg has already been calculated
			avg = filterByElemMeans.get(filterById); 
		} else { // avg needs to be calculated
			for (Rating rate: ratings.get(filterById)) {
				avg += rate.getRating();
			}
			avg /= ratings.get(filterById).size();
			// store for later reuse
			this.filterByElemMeans.put(filterById, avg);
		}
		
		return avg;
	}
	
	public int getMaxFilterById() {
		return maxFilterById;
	}
	
	public int getMaxFeatureId() {
		return maxFeatureId;
	}
	
	/*
	 * Abstract methods
	 */
		
	public abstract int getFilterByIdFromRating(Rating r);
	public abstract int getFeatureIdFromRating(Rating r);

}
