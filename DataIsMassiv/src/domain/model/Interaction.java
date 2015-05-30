package domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import domain.Rating;

public class Interaction implements DelatAccess, Serializable {
	private static final long serialVersionUID = 1L;
	private static double etaMovie = .07;
	private static double etaUser = .12;
	private static double lambda = .0005;
	private int featureSize = 250;
	private HashMap<Integer, RealVector> movies = new HashMap<>();
	private ArrayList<HashMap<Integer, RealVector>> usersTimeBased = new ArrayList<>();
	private final int daysPerBucket;

	public Interaction(int daysPerBucket, int estimateMaxOfDays) {
		this.daysPerBucket = daysPerBucket;
		for (int i = 0; i < estimateMaxOfDays / daysPerBucket; i++)
			usersTimeBased.add(new HashMap<>());
	}

	public void train(List<Rating> toTrain, BaseLearner base,
			MovieInTime movieTime, UserInTime userTime, double randomNess) {

		System.out.println("starting training on interaction");
		
		ArrayList<Thread> threads = new ArrayList<>();
		for(int i = 0; i < 4; i++){
			
			final int threadNum = i;
			threads.add(new Thread(new Runnable() {
				
				@Override
				public void run() {
					ArrayList<Rating> myTrain = new ArrayList<>(toTrain.size()/4+1);
					for(int i = threadNum; i < toTrain.size(); i += 4 ){
						myTrain.add(toTrain.get(i));
					}
					trainParallel(myTrain, base, movieTime, userTime, randomNess, new Random());
				}
			}));
		}
		
		for(Thread t: threads){
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		RealVector testMovieVector = movies.get(toTrain.get(0).getMovieId());
		System.out.println(testMovieVector);
		RealVector testUserVector = getUserVector(toTrain.get(0));
		System.out.println(testUserVector);
		System.out.println("interaction: "
				+ toScale(sigmoid(testUserVector.dotProduct(testMovieVector))));

	}

	private void trainParallel(List<Rating> toTrain, BaseLearner base,
			MovieInTime movieTime, UserInTime userTime, double randomNess,
			Random rand) {
		for (Rating rating : toTrain) {
			double teacher = rating.getRating() - base.getDelta(rating)
					- movieTime.getDelta(rating)
					- userTime.getDelta(rating);
			teacher = toSigmoid(teacher);

			double student = toSigmoid(getDelta(rating));

			double error = teacher - student;

			double gradientDelta = sigmoDiff(student) * error;
			RealVector movieVector = getMovieVector(rating);
			RealVector userVector = getUserVector(rating);

			RealVector deltaMovie = userVector.mapMultiply(gradientDelta
					* etaMovie);
			deltaMovie = deltaMovie.subtract(movieVector.mapMultiply(lambda
					* etaMovie));

			RealVector movieRandomd = movieVector.map((double x) -> x
					+ randomNess * (rand.nextDouble() - .5));
			RealVector newMovieVector = movieRandomd.add(deltaMovie);
			setMovieVector(rating, newMovieVector);

			RealVector deltaUser = movieVector.mapMultiply(gradientDelta
					* etaUser);
			deltaUser = deltaUser.subtract(userVector.mapMultiply(lambda
					* etaUser));
			RealVector userRandomd = userVector.map((double x) -> x
					+ randomNess * (rand.nextDouble() - .5));
			RealVector newUserVector = userRandomd.add(deltaUser);
			setUserVector(rating, newUserVector);
		}
	}

	private static double sigmoid(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	private static double toScale(double sigmoidOut) {
		return (sigmoidOut - .5) * 10;
	}

	private static double toSigmoid(double scale) {
		return (scale / 10) + .5;
	}

	private static double sigmoDiff(double out) {
		return out * (1 - out);
	}

	@Override
	public double getDelta(Rating rating) {
		RealVector movie = getMovieVector(rating);
		RealVector user = getUserVector(rating);
		double prediction = toScale(sigmoid(movie.dotProduct(user)));
		return prediction;
	}

	private synchronized RealVector getMovieVector(Rating rating) {
		RealVector realVector = movies.get(rating.getMovieId());
		if (realVector == null) {
			realVector = MatrixUtils.createRealVector(new double[featureSize]);
			setMovieVector(rating, realVector);
		}
		return realVector;
	}

	private synchronized RealVector getUserVector(Rating rating) {
		int bucketNo = rating.getDateId() / daysPerBucket;
		bucketNo = usersTimeBased.size() >= bucketNo ? usersTimeBased.size() - 1
				: bucketNo;

		HashMap<Integer, RealVector> hashMapOfPointInTime = usersTimeBased
				.get(bucketNo);
		RealVector realVector = hashMapOfPointInTime.get(rating.getUserId());
		if (realVector == null) {
			realVector = MatrixUtils.createRealVector(new double[featureSize]);
			hashMapOfPointInTime.put(rating.getUserId(), realVector);
		}
		return realVector;
	}

	private synchronized void setUserVector(Rating rating, RealVector newUserVector) {
		int bucketNo = rating.getDateId() / daysPerBucket;
		bucketNo = usersTimeBased.size() >= bucketNo ? usersTimeBased.size() - 1
				: bucketNo;
		usersTimeBased.get(bucketNo).put(rating.getUserId(), newUserVector);
	}

	private synchronized void setMovieVector(Rating rating, RealVector newMovieVector) {
		movies.put(rating.getMovieId(), newMovieVector);
	}
}
