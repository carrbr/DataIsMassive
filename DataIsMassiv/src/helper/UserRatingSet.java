package helper;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import domain.Rating;

/**
 * 
 * @author BrianCarr
 *
 * This class is a set of ratings organized by user, designed to allow O(1) 
 * time additions of user ratings, as well as to allow easy iteration through
 * all available user ratings
 */
public class UserRatingSet implements Iterable<ArrayList<Rating>> {
	/*
	 * The Integer key is the userID
	 * 
	 * The ArrayList of Ratings contains all ratings for that user
	 */
	private Map<Integer, ArrayList<Rating>> ratings;
	
	public UserRatingSet() {
		ratings = new Hashtable<Integer, ArrayList<Rating>>();
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
	}

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
			
			// swap out for new array list
			entry.setValue(userVectorPrime);
		}
	}
	
	/**
	 * 
	 * @param urs
	 * UserRatingSet to be merged with <code>this</code>
	 */
	public void merge(UserRatingSet urs) {
		// TODO implement this stub
	}
	
	/**
	 * Look but no touch.  Not a petting zoo.  Window shopping only.
	 * 
	 * If you modify anything through this iterator it will probably eat your soul.
	 * 
	 * AND YOU WILL DESERVE IT.
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
}
