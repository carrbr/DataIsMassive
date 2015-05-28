package domain.model;

import helper.AbstractRatingSet;
import helper.SimilarElement;
import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
	private BufferedWriter out;
	
	// TODO remove this later
	private static int failCount;
	private static int failCountNulls;
	private static int processedCount;
	static {
		failCount = 0;
		failCountNulls = 0;
		processedCount = 0;
	}
	
	/*
	 * params for our model
	 */
	private double minSim; // minimum similarity to be considered useful for predicting ratings
	private int minCount; // must have at least this many similar elements to not hedge
	private double pcWeight; // weighting (on a scale of 0..1) of pearson coefficient vs cosine similarity


	public AbstractCollaborativeFilteringModel(String trainingSetFile, int n, double minSim, int minCount, double pcWeight) {
		super();
		this.trainingSetFile = trainingSetFile;
		this.numSimilarElems = n;
		this.minSim = minSim;
		this.minCount = minCount;
		this.pcWeight = pcWeight;
		
		// we will incur model construction cost immediately upon creation
		buildModel();
	}

	public AbstractCollaborativeFilteringModel(String trainingSetFile, int n) {
		this(trainingSetFile, n, 0.02, 10, 0.5); // use default params
	}

	@Override
	public Rating predict(Rating r) {
		int filterById = trainSet.getFilterByIdFromRating(r);
		Rating result = null;
		if (processedCount % 10000 == 0) {
			System.out.println("rated " + processedCount + " elems");
		}
		processedCount++;
		try {
			if (out == null) {
				out = new BufferedWriter(new FileWriter(new File("data/" + getLogFileName())));
			}
			
			Queue<SimilarElement> simElems = similarElements.get(filterById);
			if (simElems == null || simElems.size() == 0) {
				failCount++;
				if (simElems == null) failCountNulls++;
				System.out.println("num without similar elems = " + failCount + ", num null = " + failCountNulls);
				result =  r.reRate((float) 3.0); // we don't have this user in our data so the best we can do is guess in the middle
				out.write("0,"); // no similarity
			} else {
				result = r.reRate((float)generateRatingFromSimilar(simElems, trainSet,
						filterById, trainSet.getFeatureIdFromRating(r), out));
			}
			out.write(result.toString() + "\n");
		} catch (IOException e) {
			System.err.println("Issue with log file writer");
			e.printStackTrace();
		}
		
		return result;

	}
	
	
	/**
	 * This method generates a rating using those of the elements of the similarSet to make the prediction
	 * This implementation computes a simple average.
	 * @param similarSet Contains the N most similar vectors to the vector for the given filterBaseId
	 * @param rs Rating set containing all ratings sorted by their filterById
	 * @param filterById ID for dimension we are performing collaborative filtering based on (i.e. user in user-based filtering)
	 * @param featureId ID in feature dimension (i.e. whatever component is represented in the vector space)
	 * @return
	 */
	public double generateRatingFromSimilar(Queue<SimilarElement> similarSet, AbstractRatingSet rs, int filterById, int featureId, BufferedWriter out) {
		int count = 0;
		double baseline = computeFilterByElemBaselineRating(filterById, featureId, rs);
		double result = baseline;
		
		// will use this to weight by similarity scores
		double simTotal = 0;
		for (SimilarElement simElem: similarSet) {
			if (rs.getRatingValue(simElem.id, featureId) != 0  && simElem.similarity >= minSim) { // only count if it's a rating value worth using
				simTotal += simElem.similarity; // note right now similarities can only be positive because of minSim, o.w. need to take abs()
			}
		}
		
		/*
		 * using weighted (by similarity) average of useful ratings (greating than minSim, and not unrated)
		 * from similar elems.  Also we are norming the vectors of similar elems to the same average as the
		 * elem that we are attemptimg to generate a rating for
		 * 
		 * E		- similar elements, represented by some (not all!) members of similarSet
		 * r_ei 	- rating for element e on item i, result
		 * r_base	- baseline rating for element e, see computeFilterByElemBaselineRating() for details
		 * k		- normalizing factor, 1/sum(abs(sim(e, e_prime))) for e_prime in E.  k = 1/simTotal
		 * 
		 * r_ei = r_avg_e + k * sum(sim(e, e_prime) * (r_e_primei - r_avg_e_prime)) for e_prime in E
		 * 
		 */
		double r_avg_e_prime = -1;
		double sum = 0.0;
		if (simTotal > 0) { // only rate if similar elements actually have something in common
			for (SimilarElement simElem: similarSet) {
				double r_e_primei = rs.getRatingValue(simElem.id, featureId);
				r_avg_e_prime = rs.getMeanForFilterById(simElem.id);
				if (r_e_primei != 0 && simElem.similarity >= minSim) { // only for elems who have rated this
					sum += (r_e_primei - r_avg_e_prime) * (simElem.similarity); // weight based on similarity
					//System.out.println("\tratingValue = " + ratingValue + ", weight = " + (simUser.similarity / simTotal) + ", similarity = " + simUser.similarity);
					count++;
				}
			}
			sum /= simTotal;
		} 
		result += sum;
		
		// when we have few useful similar elems, hedge our bets and bias towards the middle
		if (count <= minCount && count > 0) {
			result = hedgeBets(filterById, count, rs, result);
		}
		
		try {
			out.write(Double.toString(simTotal) + ",");
		} catch (IOException e) {
			System.err.println("Error writing to log file");
			e.printStackTrace();
		}
		
		result = getFlippedRatingIfNecessary(result, baseline);
		result = truncateIfNecessary(result); // TODO ideally scale based on deviation instead of truncating
		
		// TODO Remove this
		if (result > 5.0 || result < 1.0) {
			System.out.println("Halp: rating = " + result + " uId = " + filterById + " mId = " + featureId);
		}
		
		return result;
	}
	
	/**
	 * computes the baseline rating b_xi = u + b_x + b_i, where u is the overall average rating,
	 * b_x is the element's deviation from the average (avg_elem_x - u), and b_i is the feature's
	 * deviation from the average (avg_feature_i - u).
	 * 
	 * note: b_xi = u + b_x + b_i = u + (avg_elem_x - u) + (avg_feature_i - u) = avg_elem_x + avg_feature_i - u
	 * 
	 * @param filterById
	 * @param rs
	 * @return
	 */
	private double computeFilterByElemBaselineRating(int filterById, int featureId, AbstractRatingSet rs) {
		double result = rs.getMeanForFilterById(filterById);
		result += rs.getMeanForFeatureId(featureId);
		result -= rs.getOverallMeanRating();
		return result;
	}
	
	/**
	 * This function moves the resulting rating prediction towards the filterByElem's average
	 */
	private double hedgeBets(int filterById, int count, AbstractRatingSet rs, double prevResult) {
		double result = 0.0;
		double hedgeWeight = minCount * 1.5 + 1;
		double hedgeTotal = 2 * minCount;
		double filterByElemAvg = rs.getMeanForFilterById(filterById);
		
		hedgeWeight -= count;
//		System.out.println("hedging... count = " + count + " prevResult = " + prevResult + " resultWeight = " + ((hedgeTotal - hedgeWeight) / hedgeTotal)
//				+ " hedgeWeight = " + (hedgeWeight / hedgeTotal) + " avg = " + filterByElemAvg);
		result = filterByElemAvg * (hedgeWeight / hedgeTotal) + prevResult * ((hedgeTotal - hedgeWeight) / hedgeTotal);
		
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
		Queue<SimilarElement> simElems = new PriorityQueue<SimilarElement>();
		
		int candidateId = -1;
		for (String candidate: candidates) {
			candidateId = Integer.parseInt(candidate);
			
			if (!rs.containsFilterById(candidateId)) {
				continue; // no elem by this id in dataset
			}
			if (queryFilterById != candidateId) { // ensure we don't count self as similar user
				double currentSim = findSimilarity(queryFilterById, candidateId, rs);
				// first we fill up the PriorityQueue
				if (simElems.size() < n && currentSim > this.minSim) {
					simElems.add(new SimilarElement(candidateId, currentSim));
				} else { 
					if (currentSim > this.minSim && currentSim > simElems.peek().similarity) {
						// add this elem and drop current least similar elem
						simElems.poll();
						simElems.add(new SimilarElement(candidateId, currentSim));
					}
				}
			}
		}
		return simElems;
	}
	
	/**
	 * This method computes a similarity value between two vectors.  This particular implementation
	 * uses the cosine similarity, cos(u, v) = (u * v)/(||u|| * ||v||)
	 * 
	 * @param u user rating vector
	 * @param v second user rating vector to compute similarity to
	 * @return value for the similarity
	 */
	private double findSimilarity(int uId, int vId, AbstractRatingSet rs) {
		double pc = pearsonCorrelation(uId, vId, rs);
		double cs = cosineSimilarity(uId, vId, rs);
		return getFlipedRatingIfNeccesary(cs * (1 - pcWeight) + pc * pcWeight);
	}
	
	private double cosineSimilarity(int uId, int vId, AbstractRatingSet rs) {
		SparseVector u = rs.getNormedSparseVectorFromRatingList(uId);
		SparseVector v = rs.getNormedSparseVectorFromRatingList(vId);
		double dotProdUV = u.dot(v);
		double frobeniusNormU = u.norm(Norm.TwoRobust);
		double frobeniusNormV = v.norm(Norm.TwoRobust);
		double sim = 0.0;
		if (frobeniusNormU != 0 && frobeniusNormV != 0) { // don't want to divide by zero
			sim = dotProdUV / (frobeniusNormU * frobeniusNormV);
		}
		return sim;
	}
	
	private double pearsonCorrelation(int uId, int vId, AbstractRatingSet rs) {
		ArrayList<Rating> u = rs.getFilterByElemRatings(uId);
		double uAvg = rs.getMeanForFilterById(uId);
		ArrayList<Rating> v = rs.getFilterByElemRatings(vId);
		double vAvg = rs.getMeanForFilterById(vId);
		
		double numerator = 0.0;
		double denominator = 0.0;
		int i = 0, j = 0;
		double uSumSquare = 0.0, vSumSquare = 0.0;
		Rating uRate = u.get(i);
		Rating vRate = v.get(j);
		int uFeature = rs.getFeatureIdFromRating(uRate);
		int vFeature = rs.getFeatureIdFromRating(vRate);
		while (true) { // iterate until no more mutually rated items
			if (uFeature == vFeature) { // both have rated same item
				numerator += (vRate.getRating() - vAvg) * (uRate.getRating() - uAvg);
				uSumSquare += (uRate.getRating() - uAvg) * (uRate.getRating() - uAvg);
				vSumSquare += (vRate.getRating() - vAvg) * (vRate.getRating() - vAvg);
				if (j < v.size() && i < u.size()) {
					vRate = v.get(j++);	
					vFeature = rs.getFeatureIdFromRating(vRate);
					uRate = u.get(i++);
					uFeature = rs.getFeatureIdFromRating(uRate);
				} else {
					break; // exhausted all mutual ratings
				}
			} else if (vFeature < uFeature && j < v.size()) {
				vRate = v.get(j++);
				vFeature = rs.getFeatureIdFromRating(vRate);
			} else if (vFeature > uFeature && i < u.size()) {
				uRate = u.get(i++);
				uFeature = rs.getFeatureIdFromRating(uRate);
			} else {
				break; // exhausted all mutual ratings
			}
			
			
		}
		denominator = Math.sqrt(uSumSquare * vSumSquare);
				
		double result = 0.0; // if we would have to divide by zero, just return zero anyways
		if (denominator != 0) {
			result = numerator / denominator;
		}
		return result;
	}
	
	protected double getFlipedRatingIfNeccesary(double sim) {
		return sim;
	}
	
	protected double getFlippedRatingIfNecessary(double result, double avg) {
		return result;
	}
	
	private double truncateIfNecessary(double result) {
		if (result > 5.0) {
			result = 5.0;
		} else if (result < 1.0) {
			result = 1.0;
		}
		return result;
	}
	
	@Override
	public void close() {
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				System.err.println("Failed to close log file writer");
				e.printStackTrace();
			}
		}
	}
	
	protected abstract AbstractRatingSet generateRatingSet();
	protected abstract String getLogFileName();
}
