package helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
public class UserRatingSet implements Iterable<ArrayList<Rating>>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3370186565165421203L;
	/*
	 * The Integer key is the userID
	 * 
	 * The ArrayList of Ratings contains all ratings for that user
	 */
	private Map<Integer, ArrayList<Rating>> ratings;
//	private Map<Integer, ArrayList<Rating>> ratingsNormed;
	private int maxMovieId;
	private int maxUserId;
	
	public UserRatingSet() {
		ratings = new Hashtable<Integer, ArrayList<Rating>>();
//		ratingsNormed = new Hashtable<Integer, ArrayList<Rating>>();
		maxMovieId = 0;
		maxUserId = 0;
	}
	
	public void addUserRating(Rating r) {
		if (ratings.containsKey(r.getUserId())) {
			// user already exists in table, simply add rating to their vector
			ratings.get(r.getUserId()).add(r);
		} else { // user does not yet exist, add them
			ArrayList<Rating> userRatingVector = new ArrayList<Rating>();
			userRatingVector.add(r);
			ratings.put(r.getUserId(), userRatingVector);
		}
		
		// book keeping for IDs
		if (r.getMovieId() > this.maxMovieId) {
			this.maxMovieId = r.getMovieId();
		}
		
		if (r.getUserId() > this.maxUserId) {
			this.maxUserId = r.getUserId();
		}
	}

	/*
	public void subtractRowMeansFromEachRating() {
		Set<Map.Entry<Integer, ArrayList<Rating>>> entries = ratings.entrySet();
		Iterator<Map.Entry<Integer, ArrayList<Rating>>> entryIt = entries.iterator();
		
		// we will perform this operation on each user rating vector
		while (entryIt.hasNext()) {
			Map.Entry<Integer, ArrayList<Rating>> entry = entryIt.next();
			
			// compute average
			float avg = (float) 0.0;
			ArrayList<Rating> userVector = entry.getValue();
			for (int i = 0; i < userVector.size(); i++) {
				avg += userVector.get(i).getRating();
			}
			avg /= userVector.size();
			
			// subtract average from each element
			
			// can't modify the actual values due to Hashtable implementation so
			// put subtracted values here, then replace the old array list
			ArrayList<Rating> userVectorPrime = new ArrayList<Rating>();
			for (int i = 0; i < userVector.size(); i++) {
				userVectorPrime.add(i, userVector.get(i).reRate(userVector.get(i).getRating() - avg));
			}
			
			// place normed vector into normed data structure
			ratingsNormed.put(userVectorPrime.get(0).getUserId(), userVectorPrime);
		}
	}*/
	
	public Vector getUserRatingsAsNormedVector(int userId) {
		Vector ratingVec = new Vector(maxMovieId + 1); // vector should have same dimensionality as the number of movies
		ratingVec.setKey(Integer.toString(userId));

		if (!ratings.containsKey(userId)) {
			return null;
		}
		
		// compute average
		double avg = this.calcUserMean(userId);
		
		Rating r = ratings.get(userId).get(0);
		for (int i = 0; i <= maxMovieId; i++) {
			if (i == r.getMovieId()) { // it is now this ratings index
				ratingVec.set(i, r.getRating() - avg);
				if (i + 1 <= maxMovieId && i + 1 < ratings.get(userId).size()) {
					r = ratings.get(userId).get(i + 1); // prepare next rating
				}
			} else { // empty element in sparse rating data -- fill it in
				ratingVec.set(i, 0.0);
			}
		}
		ratingVec.setKey(Integer.toString(r.getUserId()));
		
		return ratingVec;
	}
	
	public ArrayList<Rating> getUserRatings(int userId) {
		return ratings.get(userId);
	}
	
	/**
	 * 
	 * @param urs
	 * UserRatingSet to be merged with <code>this</code>
	 */
	public float getRatingValue(int userId, int movieId) {
		ArrayList<Rating> ratingList = ratings.get(userId);
		if (ratingList == null) { // this rating does not exist
			return (float) 0.0; // TODO this is questionable
		}
		// find the correct rating
		for (Rating rating: ratingList) {
			if (rating.getMovieId() == movieId) {
				return rating.getRating();
			}
		}
		// user didn't rate this movie
		return 0; // questionable decision, may have to change this later
	}
	
	/**
	 * Look but no touch.  Not a petting zoo.  Window shopping only.
	 * 
	 * If you modify anything through this iterator it will probably eat your soul.
	 * 
	 * AND YOU WILL DESERVE IT.
	 * 
	 * Also, note that this will iterate through the lists of normed vectors, not 
	 * the actual valued vectors.
	 */
	@Override
	public Iterator<ArrayList<Rating>> iterator() {
		return new UserRatingSetIterator();
	}
	
	private class UserRatingSetIterator implements Iterator<ArrayList<Rating>> {
		Set<Map.Entry<Integer, ArrayList<Rating>>> entries = null;
		Iterator<Map.Entry<Integer, ArrayList<Rating>>> entryIt = null;
		
		public UserRatingSetIterator() {
			entries = ratings.entrySet();
			entryIt = entries.iterator();
		}
		
		@Override
		public boolean hasNext() {
			return entryIt.hasNext();
		}

		@Override
		public ArrayList<Rating> next() {
			// we don't want to return the whole entry pair, just the user vector
			return entryIt.next().getValue();
		}
		
	}
	
	public int getMaxMovieId() {
		return maxMovieId;
	}
	
	public int getMaxUserId() {
		return maxUserId;
	}

	public double calcUserMean(int userId) {
		double avg = 0.0;
		for (Rating rate: ratings.get(userId)) {
			avg += rate.getRating();
		}
		avg /= ratings.get(userId).size();
		return avg;
	}
}
