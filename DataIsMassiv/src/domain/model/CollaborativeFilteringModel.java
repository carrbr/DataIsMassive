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
	private UserRatingSet urs;
	private int numSimilarUsers;

	public CollaborativeFilteringModel(String trainingSetFile, int n) {
		super();
		this.trainingSetFile = trainingSetFile;
		this.urs = new UserRatingSet();
		this.numSimilarUsers = n;
		
		// may want to put this elsewhere later, but for now we will incur cost
		// immediately upon creation
		buildModel();
	}
	
	@Override
	public Rating predict(Rating r) {
		Queue<SimilarUser> simUsers = findNSimilarUsers();

		return r.reRate(5); // TODO: fix this...obviously
	}
	
	private void buildModel() {
		buildRatingSet();
		urs.subtractRowMeansFromEachRating();
	}
	
	private void buildRatingSet() {
		try {
			TextToRatingReader ratingReader = new TextToRatingReader(trainingSetFile);
			
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
			System.err.println("CollaborativeFilteringModel.buildModel: Failed to open training set file, at path: " + trainingSetFile);
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.err.println("CollaborativeFilteringModel.buildModel: Failed to close properly");
			e.printStackTrace();
			System.exit(1);
		}
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
	
	private Queue<SimilarUser> findNSimilarUsers() {
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
