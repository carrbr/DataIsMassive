package helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import no.uib.cipr.matrix.sparse.SparseVector;
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
	private Map<Integer, ArrayList<Rating>> ratings;
	private int maxFeatureId;
	private int maxFilterById;
	private boolean filterByMeansCalculated;
	private ArrayList<Double> filterByElemMeans;
	private boolean featureMeansCalculated;
	private ArrayList<Double> featureElemMeans;
	private ArrayList<Integer> featureElemCounts;
	private int numRatings;
	private double ratingSum;
	
	/*
	 * for storing and computing temporal mean data
	 */
	private ArrayList<Map<Integer, Float>> filterByElemTemporalMeans;
	private ArrayList<Map<Integer, Integer>> filterByElemTemporalCounts;
	private ArrayList<Map<Integer, Float>> featureElemTemporalMeans;
	private ArrayList<Map<Integer, Integer>> featureElemTemporalCounts;
	
	public AbstractRatingSet() {
		ratings = new Hashtable<Integer, ArrayList<Rating>>();
		maxFeatureId = 0;
		maxFilterById = 0;
		filterByElemMeans = new ArrayList<Double>();
		featureMeansCalculated = false;
		featureElemMeans = new ArrayList<Double>();
		featureElemCounts = new ArrayList<Integer>();
		numRatings = 0;
		ratingSum = 0.0;
		
		filterByElemTemporalMeans = new ArrayList<Map<Integer, Float>>();
		filterByElemTemporalCounts = new ArrayList<Map<Integer, Float>>();
		featureElemTemporalMeans = new ArrayList<Map<Integer, Float>>();
		featureElemTemporalCounts = new ArrayList<Map<Integer, Float>>();
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
		
		// book keeping
		numRatings++;
		ratingSum += r.getRating();
		addMeanDataForFeature(getFeatureIdFromRating(r), r.getRating());
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
	
	public ArrayList<Rating> getFilterByElemNormRatingsList(int filterById) {
		ArrayList<Rating> v = this.ratings.get(filterById); // TODO might this cause a null issue?
		float avg = (float) getMeanForFilterById(getFilterByIdFromRating(v.get(0)));
		
		// subtract average from each element
		ArrayList<Rating> norm = new ArrayList<Rating>();
		for (int i = 0; i < v.size(); i++) {
			norm.add(i, v.get(i).reRate(v.get(i).getRating() - avg));
		}
		return norm;
	}
	
	public ArrayList<Rating> getFilterByElemRatings(int filterById) {
		return ratings.get(filterById); // TODO this might return null... is that OK?
	}
	
	public SparseVector getNormedSparseVectorFromRatingList(int filterById) {
		int size = this.getMaxFeatureId() + 1;
		ArrayList<Rating> filterByElemRatings = ratings.get(filterById);
		double[] ratings = new double[filterByElemRatings.size()];
		int [] indexes = new int[filterByElemRatings.size()];
		
		// index each rating by movieID
		Rating r = null;
		for (int i = 0; i < filterByElemRatings.size(); i++) {
			r = filterByElemRatings.get(i);
			ratings[i] = r.getRating(); // TODO check this code works properly
			indexes[i] = getFeatureIdFromRating(r);
		}
		return new SparseVector(size, indexes, ratings);
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
		if (!filterByMeansCalculated) { // avg needs to be calculated
			for (int i = 0; i <= maxFilterById; i++) {
				ArrayList<Rating> ratingList = ratings.get(i);
				avg = 0.0;
				if (ratingList == null || ratingList.size() == 0) {
					avg = getOverallMeanRating();
				} else {
					for (Rating rate: ratingList) {
						avg += rate.getRating();
					}
					avg /= ratingList.size();
				}
				// be careful when storing so we don't move any other elements in the list...
				while (!(i < filterByElemMeans.size())) {
					filterByElemMeans.add(0.0);
				}
				filterByElemMeans.set(i, avg);
			}
			filterByElemMeans.trimToSize(); // this shouldn't be increasing anymore so trim down for efficiency
			filterByMeansCalculated = true;
		}
		return filterByElemMeans.get(filterById);
	}
	
	public double getMeanForFeatureId(int featureId) {
		double curr = -1.0;
		if (!featureMeansCalculated) { // avg needs to be calculated
			for (int i = 0; i <= maxFeatureId; i++) {
				curr = featureElemMeans.get(i);
				if (curr == 0) { // nothing here, just use overall avg
					featureElemMeans.set(i, getOverallMeanRating());
				} else {
					featureElemMeans.set(i, curr / featureElemCounts.get(i));
				}
			}
			// be efficient with memory, won't need extra space anymore
			featureElemMeans.trimToSize();
			featureElemCounts = null;
			featureMeansCalculated = true;
		}
		return featureElemMeans.get(featureId);
	}
	
	public double getOverallMeanRating() {
		return ratingSum / numRatings;
	}
	
	public boolean containsFilterById(int filterById) {
		return ratings.containsKey(filterById);
	}
	
	public int getMaxFilterById() {
		return maxFilterById;
	}
	
	public int getMaxFeatureId() {
		return maxFeatureId;
	}
	
	private void addMeanDataForFeature(int featureId, double rating) {
		while (!(featureId < featureElemMeans.size())) {
			featureElemMeans.add(0.0);
			featureElemCounts.add(0);
		}
		featureElemMeans.set(featureId, featureElemMeans.get(featureId) + rating);
		featureElemCounts.set(featureId, featureElemCounts.get(featureId) + 1);
	}
	
	private void addToTemporalMeanData(int filterById, int featureId, Rating rating) {
		int numBuckets = 10; // TODO evaluate if this is right, and make plumbing to parameterize
		Map<Integer, Float> mBucket = null;
		Map<Integer, Integer> cBucket = null;
		
		// use integer division to put each set of numBuckets subsequent dateIds into the same bucket
		int temporalBucket = rating.getDateId() / numBuckets;
		
		// handle filterById
		while (!(filterById < filterByElemTemporalMeans.size())) {
			filterByElemTemporalMeans.add(null);
			filterByElemTemporalCounts.add(null);
		}
		if (filterByElemTemporalMeans.get(filterById) == null) { // create new bucket
			mBucket = new TreeMap<Integer, Float>();
			mBucket.put(temporalBucket, rating.getRating());
			filterByElemTemporalMeans.set(filterById, mBucket);
			cBucket = new TreeMap<Integer, Integer>();
			cBucket.put(temporalBucket, 1);
			filterByElemTemporalCounts.set(filterById, cBucket);
		} else { // bucket exists -- update
			mBucket = filterByElemTemporalMeans.get(filterById);
			mBucket.replace(temporalBucket, mBucket.get(temporalBucket) + rating.getRating());
			cBucket = filterByElemTemporalCounts.get(filterById);
			cBucket.replace(temporalBucket, cBucket.get(temporalBucket) + 1);
		}
		
		// handle featureId
		while (!(featureId < featureElemTemporalMeans.size())) {
			featureElemTemporalMeans.add(null);
			featureElemTemporalCounts.add(null);
		}
		if (featureElemTemporalMeans.get(filterById) == null) { // create new bucket
			mBucket = new TreeMap<Integer, Float>();
			mBucket.put(temporalBucket, rating.getRating());
			featureElemTemporalMeans.set(filterById, mBucket);
			cBucket = new TreeMap<Integer, Integer>();
			cBucket.put(temporalBucket, 1);
			featureElemTemporalCounts.set(filterById, cBucket);
		} else { // bucket exists -- update
			mBucket = featureElemTemporalMeans.get(filterById);
			mBucket.replace(temporalBucket, mBucket.get(temporalBucket) + rating.getRating());
			cBucket = featureElemTemporalCounts.get(filterById);
			cBucket.replace(temporalBucket, cBucket.get(temporalBucket) + 1);
		}
	}
	
	/*
	 * Abstract methods
	 */
		
	public abstract int getFilterByIdFromRating(Rating r);
	public abstract int getFeatureIdFromRating(Rating r);

}
