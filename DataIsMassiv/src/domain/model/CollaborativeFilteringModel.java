package domain.model;

import helper.SimilarUser;
import helper.TextToRatingReader;
import helper.UserRatingSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

public class CollaborativeFilteringModel extends AbstractRatingModel {
	private static final long serialVersionUID = 8557616341507027600L;
	private String trainingSetFile;
	private UserRatingSet trainSet;
	private int numSimilarUsers;
	private ArrayList<Queue<SimilarUser>> similarUsers;  // array of groups of n users similar to user at index
	private Map<Integer, Double> userMeans;
	
	private final double minSim = 0.05;

	public CollaborativeFilteringModel(String trainingSetFile, int n) {
		super();
		this.trainingSetFile = trainingSetFile;
		this.numSimilarUsers = n;
		
		// may want to put this elsewhere later, but for now we will incur cost
		// immediately upon creation
		buildModel();
	}
	
	@Override
	public Rating predict(Rating r) {
		this.userMeans = new HashMap<Integer, Double>(); // TODO move this
		Queue<SimilarUser> simUsers = similarUsers.get(r.getUserId());
		if (simUsers == null) {
			return r.reRate((float) 3.0);
		} else {
			Rating result = r.reRate((float)generateRatingFromSimilar(simUsers, trainSet, r.getMovieId(), r.getUserId()));
			System.out.println("result = " + result.getRating() + " userId = " + r.getUserId());
			return result;
		}
	}
	
	/**
	 * This method generates a rating using those of the elements of the similarSet to make the prediciton
	 * This implementation computes a simple average.
	 * @param similarSet
	 * @param movieId
	 * @return
	 */
	public double generateRatingFromSimilar(Queue<SimilarUser> similarSet, UserRatingSet urs, int movieId, int userId) {
		double result = 0;
		int count = 0;
		int minCount = 10;
		double hedgeWeight = minCount * 1.5 + 1;
		double hedgeTotal = 2 * minCount;
		double userAvg = -1;
	
		// will use this to weight by similarity scores
		double simTotal = 0;
		for (SimilarUser simUser: similarSet) {
			if (urs.getRatingValue(simUser.id, movieId) != 0  && simUser.similarity >= minSim) { // only count if it's a rating value worth using
				simTotal += simUser.similarity; 
			}
		}
		
		if (simTotal > 0) { // only rate if similar elements actually have something in common
			for (SimilarUser simUser: similarSet) {
				double ratingValue = urs.getRatingValue(simUser.id, movieId);
				if (ratingValue != 0 && simUser.similarity >= minSim) { // only for users who have rated this
					result += ratingValue * (simUser.similarity / simTotal); // weight based on similarity
					//System.out.println("\tratingValue = " + ratingValue + ", weight = " + (simUser.similarity / simTotal) + ", similarity = " + simUser.similarity);
					count++;
				}
			}
		} 
		
		// when we have few useful similar users, hedge our bets and bias towards the middle
		if (count <= minCount && count > 0) {
			if (this.userMeans.containsKey(userId)) { // avg has already been calculated
				userAvg = userMeans.get(userId); 
			} else { // avg needs to be calculated
				userAvg = urs.calcUserMean(userId);
				this.userMeans.put(userId, userAvg);
			}
			hedgeWeight -= count;
			System.out.println("hedging... count = " + count + " prevResult = " + result + " resultWeight = " + ((hedgeTotal - hedgeWeight) / hedgeTotal)
					+ " hedgeWeight = " + (hedgeWeight / hedgeTotal) + " avg = " + userAvg);
			result = userAvg * (hedgeWeight / hedgeTotal) + result * ((hedgeTotal - hedgeWeight) / hedgeTotal);
		}
		
		if (count == 0) { // no ratings qualified.  guess in the middle
			if (this.userMeans.containsKey(userId)) { // avg has already been calculated
				userAvg = userMeans.get(userId); 
			} else { // avg needs to be calculated
				userAvg = urs.calcUserMean(userId);
				this.userMeans.put(userId, userAvg);
			}
			System.out.println("Halp, no common ground" + " using avg = " + userAvg);
			result = userAvg;
		}
		
		return result;
	}
		
	private void buildModel() {
		long start = System.currentTimeMillis();
		long endTask = 0;
		long startTask = 0;
	
		System.out.println("Building Model...\n");
		System.out.println("Building Rating Set...");
		startTask = System.currentTimeMillis();
		this.trainSet = buildRatingSet(this.trainingSetFile);
		endTask = System.currentTimeMillis();
		System.out.println("Rating Set Built in " + (endTask - startTask) / 1000 + "s\n");

		/*System.out.println("Normalizing Rating Set...");
		startTask = System.currentTimeMillis();
		//this.trainSet.subtractRowMeansFromEachRating();
		endTask = System.currentTimeMillis();
		System.out.println("Rating Set Normalized in " + (endTask - startTask) / 1000 + "s\n");*/
		
		int numMovies = trainSet.getMaxMovieId() + 1; // Note: this will work in most cases, but it really is just a heuristic.  If we have problems this will need to change
		int numUsers = trainSet.getMaxUserId() + 1;
		
		System.out.println("Finding Similar Users...");
		startTask = System.currentTimeMillis();
		this.similarUsers = initSimUserArrayList(numUsers);
		
		// build LSH table for finding similar users
		System.out.println("Building LSH Table...");
		startTask = System.currentTimeMillis();
		LSH lshTable = buildLSHTable(numUsers, numMovies);
		endTask = System.currentTimeMillis();
		System.out.println("LSH Table built in " + (endTask - startTask) / 1000 + "s\n");

		
		// we need to find our similar users for each user in the test set
		System.out.println("Finding Similar Users...");
		startTask = System.currentTimeMillis();
		Vector userRatingVec = null;
		List<String> simUserCandidates = new ArrayList<String>();
		Queue<SimilarUser> simUsers = null;
		int processedCount = 0;
		for (int i = 0; i < numUsers; i++) {
			if (processedCount % 1000 == 0) {
				endTask = System.currentTimeMillis();
				System.out.println("Processed " + processedCount + " users. " + (endTask - startTask) / 1000 + "s elapsed");
			}
			processedCount++;
			
			userRatingVec = trainSet.getUserRatingsAsNormedVector(i); // vector to use as query
			if (userRatingVec == null) { // not in dataset, skip
				continue;
			}
			simUserCandidates = lshTable.query(userRatingVec, numSimilarUsers);
			// evaluate the candidates to find the best N
			simUsers = findNSimilarUsers(numSimilarUsers, trainSet, i, simUserCandidates, numMovies);
			this.similarUsers.add(Integer.parseInt(userRatingVec.getKey()), simUsers);
		}
		endTask = System.currentTimeMillis();
		System.out.println("Similar users found  in " + (endTask - startTask) / 1000 + "s\n");
		System.out.println("Model Built in " + (endTask - start) / 1000 + "s");
	}
	
	private LSH buildLSHTable(int numVecs, int dimensions) {
		HashFamily hf = new CosineHashFamily(dimensions);
		Index index = Index.deserialize(hf, 10, 10);
		for (int i = 0; i < numVecs; i++) {
			if (i % 10000 == 0) {
				System.out.println("Added " + i + " users to LSH index");
			}
			Vector v = trainSet.getUserRatingsAsNormedVector(i);
			if (v == null) { // not in dataset, skip
				continue;
			}
			index.index(v);
		}
		
		return new LSH(index, hf);
	}
	
	private ArrayList<Queue<SimilarUser>> initSimUserArrayList(int size) {
		ArrayList<Queue<SimilarUser>> arrList = new ArrayList<Queue<SimilarUser>>(size);
		for(; size > 0; size --) {
			arrList.add(null);
		}
		return arrList;
	}
	
	private UserRatingSet buildRatingSet(String fName) {
		UserRatingSet urs = new UserRatingSet();
		
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
					urs.addUserRating(rating);
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
		
		return urs;
	}
		
	/*
	 * TODO
	 * 
	 *  This userRatingList should be normed
	 *  
	 *  Make sure we're getting normed vectors from urs here and elsewhere in the file
	 */
	
	private Queue<SimilarUser> findNSimilarUsers(int n, UserRatingSet urs, int queryUserId, List<String> candidates, int size) {
		ArrayList<Rating> userRatingList = urs.getUserRatings(queryUserId);
		ArrayList<Rating> normedUserRatingList = normVector(userRatingList);
		Queue<SimilarUser> simUsers = new PriorityQueue<SimilarUser>();
		
		// note, this assumes the number of ratings > numSimilarUsers
		int i = 0;
		ArrayList<Rating> ratingList = null;
		for (String candidate: candidates) {
			int candidateId = Integer.parseInt(candidate);
			ratingList = urs.getUserRatings(candidateId);
			if (ratingList == null) {
				continue; // no user by this id in dataset
			}
			if (userRatingList.get(0).getUserId() != ratingList.get(0).getUserId()) { // ensure we don't count self as similar user
				// first we fill up the PriorityQueue
				if (i < n) {
					simUsers.add(new SimilarUser(ratingList.get(0).getUserId(), 
							findSimilarity(sparseVectorFromRatingList(normedUserRatingList, size + 1), sparseVectorFromRatingList(ratingList, size + 1))));
				} else { 
					double currentSim = findSimilarity(sparseVectorFromRatingList(normedUserRatingList, size + 1), sparseVectorFromRatingList(ratingList, size + 1));
					if (currentSim > simUsers.peek().similarity) {
						// add this user and drop current least similar user
						simUsers.poll(); // TODO are we dropping the most similar user????
						simUsers.add(new SimilarUser(ratingList.get(0).getUserId(), currentSim));
					}
				}
			}
			i++;
		}
		return simUsers;
	}
	
	private ArrayList<Rating> normVector(ArrayList<Rating> v) {
		// compute average
		float avg = (float) 0.0;
		for (int i = 0; i < v.size(); i++) {
			avg += v.get(i).getRating();
		}
		avg /= v.size();
		
		// subtract average from each element
		ArrayList<Rating> norm = new ArrayList<Rating>();
		for (int i = 0; i < v.size(); i++) {
			norm.add(i, v.get(i).reRate(v.get(i).getRating() - avg));
		}
		return norm;
	}
	
	private SparseVector sparseVectorFromRatingList(ArrayList<Rating> userRatings, int size) {
		double[] ratings = new double[userRatings.size()];
		int [] indexes = new int[userRatings.size()];
		
		// index each rating by movieID
		Rating r = null;
		for (int i = 0; i < userRatings.size(); i++) {
			r = userRatings.get(i);
			ratings[i] = r.getRating();
			indexes[i] = r.getMovieId();
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
}
