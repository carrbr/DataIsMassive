package helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import no.uib.cipr.matrix.sparse.SparseVector;
import be.tarsos.lsh.Vector;
import domain.Rating;

/**
 * 
 * @author BrianCarr
 *
 *         This class is a set of ratings organized by user, designed to allow
 *         O(1) time additions of user ratings, as well as to allow easy
 *         iteration through all available user ratings
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
	private boolean filterByElemTemporalMeansCalculated;
	private ArrayList<Map<Integer, Float>> filterByElemTemporalMeans;
	private ArrayList<Map<Integer, Integer>> filterByElemTemporalCounts;
	private boolean featureElemTemporalMeansCalculated;
	private ArrayList<Map<Integer, Float>> featureElemTemporalMeans;
	private ArrayList<Map<Integer, Integer>> featureElemTemporalCounts;

	private static final int numWeeksPerBin = 10;
	// ideally this value would be generated at runtime, but computing the date
	// biases efficiently currently precludes that
	private static final int maxDateId = 5114;
	private static double numBins = Math.ceil((double) maxDateId / 7); // number
																		// of
																		// weeks
																		// of
																		// data
	private static int elemsPerBin = (int) Math.ceil(maxDateId / numBins);

	static {
		numBins = Math.ceil((double) maxDateId / 7); // number of weeks of data
		numBins = Math.ceil((double) numBins / numWeeksPerBin);
		elemsPerBin = (int) Math.ceil(maxDateId / numBins);
	}

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
		filterByElemTemporalCounts = new ArrayList<Map<Integer, Integer>>();
		featureElemTemporalMeans = new ArrayList<Map<Integer, Float>>();
		featureElemTemporalCounts = new ArrayList<Map<Integer, Integer>>();
	}

	public void addFilterByElemRating(Rating r) {
		int filterById = getFilterByIdFromRating(r);
		int featureId = getFeatureIdFromRating(r);

		if (ratings.containsKey(filterById)) {
			// filterById already exists in table, simply add rating to their
			// vector
			ratings.get(filterById).add(r);
		} else { // filterById does not yet exist, add them
			ArrayList<Rating> ratingVector = new ArrayList<Rating>();
			ratingVector.add(r);
			ratings.put(filterById, ratingVector);
		}

		// book keeping
		numRatings++;
		ratingSum += r.getRating();
		addMeanDataForFeature(featureId, r.getRating());
		addToTemporalMeanData(filterById, featureId, r);
		if (featureId > this.maxFeatureId) {
			this.maxFeatureId = featureId;
		}
		if (filterById > this.maxFilterById) {
			this.maxFilterById = filterById;
		}
	}

	public void trimToSize() {
		for (ArrayList<Rating> r : ratings.values()) {
			r.trimToSize();
		}
	}

	public Vector getFilterByElemRatingsAsNormedVector(int filterById) {
		Vector ratingVec = new Vector(maxFeatureId + 1); // vector should have
															// same
															// dimensionality as
															// the number of
															// movies
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
				if (i + 1 < ratings.get(filterById).size()
						&& i + 1 <= maxFeatureId) {
					r = ratings.get(filterById).get(i + 1); // prepare next
															// rating
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
		ArrayList<Rating> v = this.ratings.get(filterById); // TODO might this
															// cause a null
															// issue?
		float avg = (float) getMeanForFilterById(getFilterByIdFromRating(v
				.get(0)));

		// subtract average from each element
		ArrayList<Rating> norm = new ArrayList<Rating>();
		for (int i = 0; i < v.size(); i++) {
			norm.add(i, v.get(i).reRate(v.get(i).getRating() - avg));
		}
		return norm;
	}

	public ArrayList<Rating> getFilterByElemRatings(int filterById) {
		return ratings.get(filterById); // TODO this might return null... is
										// that OK?
	}

	public SparseVector getNormedSparseVectorFromRatingList(int filterById) {
		int size = this.getMaxFeatureId() + 1;
		ArrayList<Rating> filterByElemRatings = ratings.get(filterById);
		double[] ratings = new double[filterByElemRatings.size()];
		int[] indexes = new int[filterByElemRatings.size()];

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
		for (Rating rating : ratingList) {
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
					for (Rating rate : ratingList) {
						avg += rate.getRating();
					}
					avg /= ratingList.size();
				}
				// be careful when storing so we don't move any other elements
				// in the list...
				while (!(i < filterByElemMeans.size())) {
					filterByElemMeans.add(0.0);
				}
				filterByElemMeans.set(i, avg);
			}
			filterByElemMeans.trimToSize(); // this shouldn't be increasing
											// anymore so trim down for
											// efficiency
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

	public double getTemporalMeanForFilterById(int filterById, int dateId) {
		if (!filterByElemTemporalMeansCalculated) { // must compute averages
			computeTemporalMeans(filterByElemTemporalMeans,
					filterByElemTemporalCounts, getMaxFilterById());
			// clean up memory a bit
			filterByElemTemporalMeans.trimToSize();
			filterByElemTemporalCounts = null;
			filterByElemTemporalMeansCalculated = true;
		}
		return getTemporalMean(filterByElemTemporalMeans, filterById, dateId);
	}

	public double getTemporalMeanForFeatureId(int featureId, int dateId) {
		if (!featureElemTemporalMeansCalculated) { // must compute averages
			computeTemporalMeans(featureElemTemporalMeans,
					featureElemTemporalCounts, getMaxFeatureId());
			// clean up memory a bit
			featureElemTemporalMeans.trimToSize();
			featureElemTemporalCounts = null;
			featureElemTemporalMeansCalculated = true;
		}
		return getTemporalMean(featureElemTemporalMeans, featureId, dateId);
	}

	private void computeTemporalMeans(ArrayList<Map<Integer, Float>> elemMeans,
			ArrayList<Map<Integer, Integer>> elemCounts, int maxElemId) {
		Map<Integer, Float> sums = null;
		Map<Integer, Integer> counts = null;
		for (int i = 0; i <= maxElemId; i++) {
			sums = elemMeans.get(i);
			counts = elemCounts.get(i);
			if (sums == null || counts == null || sums.size() != counts.size()) { // no
																					// elem
																					// data
																					// here,
																					// or
																					// a
																					// problem.
																					// just
																					// skip
				continue;
			}
			for (int j = 0; j <= numBins; j++) {
				if (sums.containsKey(j) && counts.containsKey(j)) { // ensure
																	// that data
																	// bin
																	// exists
					sums.replace(j, sums.get(j) / counts.get(j));
				}
			}
		}
	}

	private float getTemporalMean(ArrayList<Map<Integer, Float>> elemMeans,
			int elemId, int dateId) {
		float avg = -1;
		int dateBinId = calculateDateBinId(dateId);
		Map<Integer, Float> means = elemMeans.get(elemId);
		if (means == null || !means.containsKey(dateBinId)) { // no data for
																// this
																// element/bin
																// combo, use
																// overall mean
			avg = (float) getOverallMeanRating();
		} else {
			avg = means.get(dateBinId);
		}
		return avg;
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

	public int calculateDateBinId(int dateId) {
		return (int) dateId / elemsPerBin;
	}

	private void addMeanDataForFeature(int featureId, double rating) {
		while (!(featureId < featureElemMeans.size())) {
			featureElemMeans.add(0.0);
			featureElemCounts.add(0);
		}
		featureElemMeans.set(featureId, featureElemMeans.get(featureId)
				+ rating);
		featureElemCounts.set(featureId, featureElemCounts.get(featureId) + 1);
	}

	private void addToTemporalMeanData(int filterById, int featureId,
			Rating rating) {
		addToTemporalMeanData(filterByElemTemporalMeans,
				filterByElemTemporalCounts, filterById, rating.getDateId(),
				rating.getRating());
		addToTemporalMeanData(featureElemTemporalMeans,
				featureElemTemporalCounts, featureId, rating.getDateId(),
				rating.getRating());
	}

	private void addToTemporalMeanData(ArrayList<Map<Integer, Float>> elemSums,
			ArrayList<Map<Integer, Integer>> elemCounts, int elemId,
			int dateId, float rating) {
		Map<Integer, Float> elemSumBins = null;
		Map<Integer, Integer> elemCountBins = null;

		// use integer division to put each set of numBuckets subsequent dateIds
		// into the same bucket
		int dateBinId = calculateDateBinId(dateId);

		// handle filterById
		while (!(elemId < elemSums.size())) {
			elemSums.add(null);
			elemCounts.add(null);
		}
		if (elemSums.get(elemId) == null) { // no bins for elem, make them
			elemSumBins = new HashMap<Integer, Float>();
			elemSumBins.put(dateBinId, rating);
			elemSums.set(elemId, elemSumBins);
			elemCountBins = new HashMap<Integer, Integer>();
			elemCountBins.put(dateBinId, 1);
			elemCounts.set(elemId, elemCountBins);
		} else {
			elemSumBins = elemSums.get(elemId);
			elemCountBins = elemCounts.get(elemId);
			if (elemSumBins.containsKey(dateBinId)
					&& elemCountBins.containsKey(dateBinId)) { // bin exists --
																// update
				elemSumBins.replace(dateBinId, elemSumBins.get(dateBinId)
						+ rating);
				elemCountBins.replace(dateBinId,
						elemCountBins.get(dateBinId) + 1);
			} else { // need new bin
				elemSumBins.put(dateBinId, rating);
				elemCountBins.put(dateBinId, 1);
			}
		}
	}

	/*
	 * Abstract methods
	 */

	public abstract int getFilterByIdFromRating(Rating r);

	public abstract int getFeatureIdFromRating(Rating r);

}
