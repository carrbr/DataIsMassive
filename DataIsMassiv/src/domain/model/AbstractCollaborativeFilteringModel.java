package domain.model;

import helper.AbstractRatingSet;
import helper.SimilarElement;
import helper.TextToRatingReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import be.tarsos.lsh.Index;
import be.tarsos.lsh.LSH;
import be.tarsos.lsh.Vector;
import be.tarsos.lsh.families.CosineHashFamily;
import be.tarsos.lsh.families.HashFamily;
import no.uib.cipr.matrix.Vector.Norm;
import no.uib.cipr.matrix.sparse.SparseVector;
import domain.Rating;

public abstract class AbstractCollaborativeFilteringModel extends AbstractRatingModel {
	private static final long serialVersionUID = 8557616341507027600L;
	protected String trainingSetFile;
	protected AbstractRatingSet trainSet;
	protected int numSimilarElems;
	protected ArrayList<Queue<SimilarElement>> similarElements;  // array of groups of n users similar to user at index
	
	/*
	 * params for our model
	 */
	protected final double minSim = 0.05; // minimum similarity to be considered useful for predicting ratings

	// TODO should ideally add constructors that allow us to take arbitrary model parameters, but for now see above
	public AbstractCollaborativeFilteringModel(String trainingSetFile, int n) {
		super();
		this.trainingSetFile = trainingSetFile;
		this.numSimilarElems = n;

		// we will incur model construction cost immediately upon creation
		buildModel();
	}
	
	@Override
	public Rating predict(Rating r) {
		Queue<SimilarElement> simMovies = similarElements.get(r.getUserId());
		if (simMovies == null) {
			return r.reRate((float) 3.0);
		} else {
			Rating result = r.reRate((float)generateRatingFromSimilar(simMovies, trainSet,
					trainSet.getFilterByIdFromRating(r), trainSet.getFeatureIdFromRating(r)));
			System.out.println("result = " + result.getRating() + " userId = " + r.getUserId());
			return result;
		}
	}
	
	
	/**
	 * This method generates a rating using those of the elements of the similarSet to make the prediction
	 * This implementation computes a simple average.
	 * @param similarSet Contains the N most similar vectors to the vector for the given filterBaseId
	 * @param rs Rating set containing all ratings sorted by their filterById
	 * @param filterBaseId ID for dimension we are performing collaborative filtering based on (i.e. user in user-based filtering)
	 * @param featureId ID in feature dimension (i.e. whatever component is represented in the vector space)
	 * @return
	 */
	public double generateRatingFromSimilar(Queue<SimilarElement> similarSet, AbstractRatingSet rs, int filterBaseId, int featureId) {
		double result = 0;
		int count = 0;
		int minCount = 15;
		double hedgeWeight = minCount * 1.5 + 1;
		double hedgeTotal = 2 * minCount;
		double filterByElemAvg = -1;
	
		// TODO modify to normalize other users whose ratings we are borrowing to generate new scores for users
		
		// will use this to weight by similarity scores
		double simTotal = 0;
		for (SimilarElement simElem: similarSet) {
			if (rs.getRatingValue(simElem.id, featureId) != 0  && simElem.similarity >= minSim) { // only count if it's a rating value worth using
				simTotal += simElem.similarity; 
			}
		}
		
		if (simTotal > 0) { // only rate if similar elements actually have something in common
			for (SimilarElement simElem: similarSet) {
				double ratingValue = rs.getRatingValue(simElem.id, featureId);
				if (ratingValue != 0 && simElem.similarity >= minSim) { // only for elems who have rated this
					result += ratingValue * (simElem.similarity / simTotal); // weight based on similarity
					//System.out.println("\tratingValue = " + ratingValue + ", weight = " + (simUser.similarity / simTotal) + ", similarity = " + simUser.similarity);
					count++;
				}
			}
		} 
		
		// when we have few useful similar elems, hedge our bets and bias towards the middle
		if (count <= minCount && count > 0) {
			filterByElemAvg = rs.getMeanForFilterById(filterBaseId);
			hedgeWeight -= count;
			System.out.println("hedging... count = " + count + " prevResult = " + result + " resultWeight = " + ((hedgeTotal - hedgeWeight) / hedgeTotal)
					+ " hedgeWeight = " + (hedgeWeight / hedgeTotal) + " avg = " + filterByElemAvg);
			result = filterByElemAvg * (hedgeWeight / hedgeTotal) + result * ((hedgeTotal - hedgeWeight) / hedgeTotal);
		}
		
		if (count == 0) { // no ratings qualified.  guess in the middle
			filterByElemAvg = rs.getMeanForFilterById(filterBaseId);
			System.out.println("Halp, no common ground" + " using avg = " + filterByElemAvg);
			result = filterByElemAvg;
		}
		
		return result;
	}
		
	protected void buildModel() {
		long start = System.currentTimeMillis();
		long endTask = 0;
		long startTask = 0;
	
		System.out.println("Building Model...\n");
		System.out.println("Building Rating Set...");
		startTask = System.currentTimeMillis();
		this.trainSet = buildRatingSet(this.trainingSetFile);
		endTask = System.currentTimeMillis();
		System.out.println("Rating Set Built in " + (endTask - startTask) / 1000 + "s\n");
		
		int numFeatureElems = trainSet.getMaxFeatureId() + 1; // Note: this will work in most cases, but it really is just a heuristic.  If we have problems this will need to change
		int numFilterByElems = trainSet.getMaxFilterById() + 1;
		
		System.out.println("Finding Similar Elements...");
		startTask = System.currentTimeMillis();
		this.similarElements = initSimFilterByElemArrayList(numFilterByElems);
		
		// build LSH table for finding similar users
		System.out.println("Building LSH Table...");
		startTask = System.currentTimeMillis();
		LSH lshTable = buildLSHTable(numFilterByElems, numFeatureElems);
		endTask = System.currentTimeMillis();
		System.out.println("LSH Table built in " + (endTask - startTask) / 1000 + "s\n");

		
		// we need to find our similar elems for each elem in the test set
		System.out.println("Finding Similar Elems...");
		startTask = System.currentTimeMillis();
		Vector filterByElemRatingVec = null;
		List<String> simElemCandidates = new ArrayList<String>();
		Queue<SimilarElement> simElems = null;
		int processedCount = 0;
		for (int i = 0; i < numFilterByElems; i++) {
			if (processedCount % 1000 == 0) {
				endTask = System.currentTimeMillis();
				System.out.println("Processed " + processedCount + " elems. " + (endTask - startTask) / 1000 + "s elapsed");
			}
			processedCount++;
			
			filterByElemRatingVec = trainSet.getFilterByElemRatingsAsNormedVector(i); // vector to use as query
			if (filterByElemRatingVec == null) { // not in dataset, skip
				continue;
			}
			simElemCandidates = lshTable.query(filterByElemRatingVec, numSimilarElems);
			// evaluate the candidates to find the best N
			simElems = findNSimilarElems(numSimilarElems, trainSet, i, simElemCandidates, numFeatureElems);
			this.similarElements.add(Integer.parseInt(filterByElemRatingVec.getKey()), simElems);
		}
		endTask = System.currentTimeMillis();
		System.out.println("Similar elems found  in " + (endTask - startTask) / 1000 + "s\n");
		System.out.println("Model Built in " + (endTask - start) / 1000 + "s");
	}
	
	private LSH buildLSHTable(int numVecs, int dimensions) {
		HashFamily hf = new CosineHashFamily(dimensions);
		Index index = Index.deserialize(hf, 10, 10);
		for (int i = 0; i < numVecs; i++) {
			if (i % 10000 == 0) {
				System.out.println("Added " + i + " elems to LSH index");
			}
			Vector v = trainSet.getFilterByElemRatingsAsNormedVector(i);
			if (v == null) { // not in dataset, skip
				continue;
			}
			index.index(v);
		}
		
		return new LSH(index, hf);
	}
	
	private ArrayList<Queue<SimilarElement>> initSimFilterByElemArrayList(int size) {
		ArrayList<Queue<SimilarElement>> arrList = new ArrayList<Queue<SimilarElement>>(size);
		for(; size > 0; size --) {
			arrList.add(null);
		}
		return arrList;
	}
	
	private AbstractRatingSet buildRatingSet(String fName) {
		AbstractRatingSet rs = generateRatingSet();
		
		try {
			TextToRatingReader ratingReader = new TextToRatingReader(fName);
			
			// build up our rating set
			Rating rating = null;
			while (true) {
				try {
					rating = ratingReader.readNext();
					if (rating == null) {
						break;
					}
					rs.addFilterByElemRating(rating);
				} catch (IOException e) {
					System.err.println("CollaborativeFilteringModel.buildModel: Error reading ratings");
					e.printStackTrace();
					System.exit(1);
				}
			}
			ratingReader.close();
		} catch (FileNotFoundException e) {
			System.err.println("CollaborativeFilteringModel.buildModel: Failed to open training set file, at path: " + fName);
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.println("CollaborativeFilteringModel.buildModel: Failed to close properly");
			e.printStackTrace();
			System.exit(1);
		}
		
		return rs;
	}
	
	private Queue<SimilarElement> findNSimilarElems(int n, AbstractRatingSet rs, int queryFilterById, List<String> candidates, int numFeatureElems) {
		ArrayList<Rating> filterByElemRatingList = rs.getFilterByElemRatings(queryFilterById);
		ArrayList<Rating> normedFilterByElemRatingList = normVector(filterByElemRatingList);
		Queue<SimilarElement> simElems = new PriorityQueue<SimilarElement>();
		
		// note, this assumes the number of ratings > numSimilarElems
		int i = 0;
		ArrayList<Rating> ratingList = null;
		for (String candidate: candidates) {
			int candidateId = Integer.parseInt(candidate);
			ratingList = rs.getFilterByElemRatings(candidateId);
			if (ratingList == null) {
				continue; // no elem by this id in dataset
			}
			if (filterByElemRatingList.get(0).getUserId() != ratingList.get(0).getUserId()) { // ensure we don't count self as similar user
				// first we fill up the PriorityQueue
				if (i < n) {
					simElems.add(new SimilarElement(rs.getFilterByIdFromRating(ratingList.get(0)), 
							findSimilarity(sparseVectorFromRatingList(normedFilterByElemRatingList, numFeatureElems),
										sparseVectorFromRatingList(ratingList, numFeatureElems))));
				} else { 
					double currentSim = findSimilarity(sparseVectorFromRatingList(normedFilterByElemRatingList, numFeatureElems),
							sparseVectorFromRatingList(ratingList, numFeatureElems + 1));
					if (currentSim > simElems.peek().similarity) {
						// add this elem and drop current least similar elem
						simElems.poll();
						simElems.add(new SimilarElement(rs.getFilterByIdFromRating(ratingList.get(0)), currentSim));
					}
				}
			}
			i++;
		}
		return simElems;
	}
	
	private ArrayList<Rating> normVector(ArrayList<Rating> v) {
		float avg = (float) trainSet.getMeanForFilterById(trainSet.getFilterByIdFromRating(v.get(0)));
		
		// subtract average from each element
		ArrayList<Rating> norm = new ArrayList<Rating>();
		for (int i = 0; i < v.size(); i++) {
			norm.add(i, v.get(i).reRate(v.get(i).getRating() - avg));
		}
		return norm;
	}
	
	private SparseVector sparseVectorFromRatingList(ArrayList<Rating> filterByElemRatings, int size) {
		double[] ratings = new double[filterByElemRatings.size()];
		int [] indexes = new int[filterByElemRatings.size()];
		
		// index each rating by movieID
		Rating r = null;
		for (int i = 0; i < filterByElemRatings.size(); i++) {
			r = filterByElemRatings.get(i);
			ratings[i] = r.getRating(); // TODO check this code works properly
			indexes[i] = trainSet.getFeatureIdFromRating(r);
		}
		return new SparseVector(size, indexes, ratings);
	}
	
	/**
	 * This method computes a similarity value between two vectors.  This particular implementation
	 * uses the cosine similarity, cos(u, v) = (u * v)/(||u|| * ||v||)
	 * 
	 * @param u user rating vector
	 * @param v second user rating vector to compute similarity to
	 * @return value for the similarity
	 */
	private double findSimilarity(SparseVector u, SparseVector v) {
		double dotProdUV = u.dot(v);
		double frobeniusNormU = u.norm(Norm.TwoRobust);
		double frobeniusNormV = v.norm(Norm.TwoRobust);
		double sim = 0.0;
		if (frobeniusNormU != 0 && frobeniusNormV != 0) { // don't want to divide by zero
			sim = dotProdUV / (frobeniusNormU * frobeniusNormV);
		}
		return sim;
	}
	
	protected abstract AbstractRatingSet generateRatingSet();
}
