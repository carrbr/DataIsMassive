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

	private final int featureSize;

	private HashMap<Integer, RealVector> movies = new HashMap<>();
	private ArrayList<HashMap<Integer, RealVector>> usersTimeBased = new ArrayList<>();
	private final int daysPerBucket;

	public static class LearningSpecs {
		public double etaMovie = .01;
		public double etaUser = .05;
		public double lambda = .0005;
		public double randomNess = 0;
	}

	public Interaction(int daysPerBucket, int estimateMaxOfDays, int featureSize) {
		this.daysPerBucket = daysPerBucket;
		this.featureSize = featureSize;
		for (int i = 0; i < estimateMaxOfDays / daysPerBucket; i++)
			usersTimeBased.add(new HashMap<>());
	}

	public void train(List<Rating> toTrain, BaseLearner base,
			MovieInTime movieTime, UserInTime userTime, LearningSpecs specs) {

		System.out.println("starting training on interaction");

		ArrayList<Thread> threads = new ArrayList<>();
		int numberOfThreads = 4;
		for (int i = 0; i < numberOfThreads; i++) {

			final int threadNum = i;
			threads.add(new Thread(new Runnable() {

				@Override
				public void run() {

					ArrayList<Rating> myTrain = new ArrayList<>(toTrain.size()
							/ numberOfThreads + 1);
					for (int i = threadNum; i < toTrain.size(); i += numberOfThreads) {
						myTrain.add(toTrain.get(i));
					}
					trainParallel(myTrain, base, movieTime, userTime,
							new Random(), specs);
				}
			}));
		}
		for (Thread t : threads) {
			t.start();
		}
		System.out.println(threads.size() + " Threads Started");
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(threads.size() + " Threads Joined");

		RealVector testMovieVector = movies.get(toTrain.get(0).getMovieId());
		System.out.println(testMovieVector);
		RealVector testUserVector = getUserVector(toTrain.get(0));
		System.out.println(testUserVector);
		System.out.println("interaction: "
				+ toScale(sigmoid(testUserVector.dotProduct(testMovieVector))));

	}

	private void trainParallel(List<Rating> toTrain, BaseLearner base,
			MovieInTime movieTime, UserInTime userTime, Random rand,
			LearningSpecs specs) {

		for (Rating rating : toTrain) {
			double teacher = rating.getRating() - base.getDelta(rating)
					- movieTime.getDelta(rating) - userTime.getDelta(rating);
			teacher = toSigmoid(teacher);

			double student = toSigmoid(getDelta(rating));

			double error = teacher - student;

			double gradientDelta = sigmoDiff(student) * error;
			RealVector movieVector = getMovieVector(rating);
			RealVector userVector = getUserVector(rating);
			
			RealVector deltaMovie = userVector.mapMultiply(gradientDelta
					* specs.etaMovie);

			deltaMovie = deltaMovie.subtract(movieVector
					.mapMultiply(specs.lambda * specs.etaMovie));

			RealVector movieRandomd = movieVector.map((double x) -> x
					+ specs.randomNess * (rand.nextDouble() - .5));

			RealVector newMovieVector = movieRandomd.add(deltaMovie);

			if (newMovieVector.isNaN()) {
				System.out.println("nMV problem");
				System.out.println(rating.getRating());
				System.out.println(base.getDelta(rating));
				System.out.println(movieTime.getDelta(rating));
				System.out.println(userTime.getDelta(rating));
				System.out.println(teacher);
				System.out
						.println("movie " + movieVector.isNaN() + movieVector);
				System.out.println("user " + userVector.isNaN() + userVector);
				throw new RuntimeException("gradient!");
			}
			setMovieVector(rating, newMovieVector);

			RealVector deltaUser = movieVector.mapMultiply(gradientDelta
					* specs.etaUser);

			deltaUser = deltaUser.subtract(userVector.mapMultiply(specs.lambda
					* specs.etaUser));

			RealVector userRandomd = userVector.map((double x) -> x
					+ specs.randomNess * (rand.nextDouble() - .5));

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
		return (scale / 10.0) + .5;
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

	private synchronized void setMovieVector(Rating rating,
			RealVector newMovieVector) {
		movies.put(rating.getMovieId(), newMovieVector);
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

	private synchronized void setUserVector(Rating rating,
			RealVector newUserVector) {
		int bucketNo = rating.getDateId() / daysPerBucket;
		bucketNo = usersTimeBased.size() >= bucketNo ? usersTimeBased.size() - 1
				: bucketNo;
		usersTimeBased.get(bucketNo).put(rating.getUserId(), newUserVector);
	}

}
