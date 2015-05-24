package domain.model;

import helper.SimilarUser;
import helper.TextToRatingReader;
import helper.UserRatingSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import no.uib.cipr.matrix.Vector.Norm;
import no.uib.cipr.matrix.sparse.SparseVector;
import domain.Rating;

public class CollaborativeFilteringModel extends AbstractRatingModel {
	private static final long serialVersionUID = 8557616341507027600L;
	private String trainingSetFile;
	private UserRatingSet trainSet;
	private int numSimilarUsers;
	private ArrayList<Queue<SimilarUser>> similarUsers;  // array of groups of n users similar to user at index

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
		Queue<SimilarUser> simUsers = similarUsers.get(r.getUserId());
		if (simUsers == null) {
			return r.reRate((float) 3.0);
		} else {
			Rating result = r.reRate((float)generateRatingFromSimilar(simUsers, trainSet, r.getMovieId()));
			return result;
		}
	}
		
	private void buildModel() {
		System.out.println("Building Model...");
		this.trainSet = buildRatingSet(this.trainingSetFile);
		System.out.println("Rating Set Built");

		this.trainSet.subtractRowMeansFromEachRating();
		System.out.println("Rating Set Normalized");
		
		int numMovies = trainSet.getMaxMovieId(); // Note: this will work in most cases, but it really is just a heuristic.  If we have problems this will need to change
		
		System.out.println("Finding Similar Users...");
		this.similarUsers = initSimUserArrayList(trainSet.getMaxUserId() + 1);
		// we need to find our similar users for each user in the test set
		Iterator<ArrayList<Rating>> trainIt = trainSet.iterator();
		ArrayList<Rating> userRatingList = null;
		Queue<SimilarUser> simUsers = null;
		while (trainIt.hasNext()) {
			userRatingList = trainIt.next();
			simUsers = findNSimilarUsers(numSimilarUsers, this.trainSet, userRatingList, numMovies);
			this.similarUsers.add(userRatingList.get(0).getUserId(), simUsers);
		}
		System.out.println("Model Built");
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
	
	/**
	 * This method generates a rating using those of the elements of the similarSet to make the prediciton
	 * This implementation computes a simple average.
	 * @param similarSet
	 * @param movieId
	 * @return
	 */
	public double generateRatingFromSimilar(Queue<SimilarUser> similarSet, UserRatingSet urs, int movieId) {
		double result = 0;
		int count = 0;
		// make sure that our similar set is preserved properly for future ratings
		Queue<SimilarUser> copySimilarSet = new PriorityQueue<SimilarUser>(similarSet);
		
		/*
		 * TODO look into our choice of 3.0 for empty ratings
		 * 
		 * also consider weighting by similarity (and an investigation of the similar users we select
		 * is probably in order too)
		 */
		
		
		SimilarUser simUser = null;
		while (!copySimilarSet.isEmpty()) {
			simUser = copySimilarSet.remove();
			double ratingValue = urs.getRatingValue(simUser.id, movieId);
			if (ratingValue != 0) { // skip similar users who have not rated this
				result += ratingValue;
				count++;
				System.out.print(ratingValue + " ");
			}
		}
		if (count == 0) {
			result = 3.0;
		} else {
			result /= count;
		}
		System.out.println("avg = " + result + "/" + count + " = " + result / count);
		return result;
	}
		
	/*
	 * TODO
	 * 
	 *  This userRatingList should be normed
	 *  
	 *  Make sure we're getting normed vectors from urs here and elsewhere in the file
	 */
	
	private Queue<SimilarUser> findNSimilarUsers(int n, UserRatingSet urs, ArrayList<Rating> userRatingList, int size) {
		Queue<SimilarUser> simUsers = new PriorityQueue<SimilarUser>();
		Iterator<ArrayList<Rating>> ursIt = urs.iterator();
		ArrayList<Rating> normedUserRatingList = normVector(userRatingList);
		
		// note, this assumes the number of ratings > numSimilarUsers
		int i = 0;
		ArrayList<Rating> ratingList = null;
		while (ursIt.hasNext()) {
			ratingList = ursIt.next();
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
		double sim = dotProdUV / (frobeniusNormU * frobeniusNormV);
		return sim;
	}
}
