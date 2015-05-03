package domain.model;

import helper.TextToRatingReader;
import helper.UserRatingSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import domain.Rating;

public class CollaborativeFilteringModel extends AbstractRatingModel {
	private static final long serialVersionUID = 8557616341507027600L;
	private String trainingSetFile;
	private String testSetFile;
	private UserRatingSet trainSet;
	private UserRatingSet testSet;
	private int numSimilarUsers;

	public CollaborativeFilteringModel(String trainingSetFile, String testSetFile, int n) {
		super();
		this.trainingSetFile = trainingSetFile;
		this.testSetFile = testSetFile;
		this.numSimilarUsers = n;
		
		// may want to put this elsewhere later, but for now we will incur cost
		// immediately upon creation
		buildModel();
	}
	
	@Override
	public Rating predict(Rating r) {

		return r.reRate(5); // TODO: fix this...obviously
	}
	
	private void buildModel() {
		trainSet = buildRatingSet(this.trainingSetFile);
		testSet = buildTestRatingSet(this.testSetFile, trainSet);
		trainSet.subtractRowMeansFromEachRating();
		// we need to find our similar users for each user in the test set
		Queue<SimilarUser> simUsers = findNSimilarUsers(trainSet);
	}
	
	/**
	 * 
	 * @param fname
	 * this is the filename for the test set data
	 * @param tset
	 * this should be a UserRatingSet containing the corresponding training set data
	 * @return
	 * This function will return a UserRatingSet containing the test set data (which doesn't
	 * contain movie rating values), merged with the training set data
	 */
	private UserRatingSet buildTestRatingSet(String fname, UserRatingSet tset) {		
		// build up the basic data structure
		UserRatingSet urs = buildRatingSet(fname);
		
		// fill in all the missing movie rating data for each user in test set using training set
		urs.merge(tset);
		
		return urs;
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
	 * nested class for storing userIds and their similarity to the queried user together 
	 */
	private class SimilarUser {
		public int id;
		public float similarity;
		
		public SimilarUser(int id, float similarity) {
			this.id = id;
			this.similarity = similarity;
		}
	}
	
	private Queue<SimilarUser> findNSimilarUsers(UserRatingSet urs, ArrayList<Rating> user) {
		Queue<SimilarUser> simUsers = new PriorityQueue<SimilarUser>();
		Iterator<ArrayList<Rating>> ursIt = urs.iterator();
		
		// note, this assumes the number of ratings > numSimilarUsers
		int i = 0;
		ArrayList<Rating> userVector = null;
		while (ursIt.hasNext()) {
			userVector = ursIt.next();
			// first we fill up the PriorityQueue
			if (i < numSimilarUsers) {
				simUsers.add(new SimilarUser(userVector.get(0).getUserId(), findSimilarity(userVector, userVector))); // TODO replace one vector arg
			} else { 
				float currentSim = findSimilarity(userVector, userVector); // TODO fix this line
				if (currentSim > simUsers.peek().similarity) {
					// add this user and drop current least similar user
					simUsers.poll();
					simUsers.add(new SimilarUser(userVector.get(0).getUserId(), currentSim));
				}
			}
			i++;
		}
		return simUsers;
	}
	
	private float findSimilarity(ArrayList<Rating> u, ArrayList<Rating> v) {
		// TODO this is only a stub
		return (float) 0.0;
	}
}
