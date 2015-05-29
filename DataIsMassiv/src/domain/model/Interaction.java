package domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import domain.Rating;

public class Interaction implements DelatAccess, Serializable {
	private static final long serialVersionUID = 1L;
	private static double etaMovie = .05;
	private static double etaUser = .15;
	private static double lambda = .0001;
	private int featureSize = 50;
	private HashMap<Integer, RealVector> movies = new HashMap<>();
	private HashMap<Integer, RealVector> users = new HashMap<>();

	public void train(List<Rating> toTrain, BaseLearner base,
			MovieInTime movieTime, UserInTime userTime, double randomNess) {
		Random rand = new Random();

		System.out.println("starting training on interaction");
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
					* etaMovie);
			deltaMovie = deltaMovie.subtract(movieVector.mapMultiply(lambda
					* etaMovie));

			RealVector movieRandomd = movieVector.map((double x) -> x
					+ randomNess * (rand.nextDouble() - .5));
			movies.put(rating.getMovieId(), movieRandomd.add(deltaMovie));

			RealVector deltaUser = movieVector.mapMultiply(gradientDelta
					* etaUser);
			deltaUser = deltaUser.subtract(userVector.mapMultiply(lambda
					* etaUser));
			RealVector userRandomd = userVector.map((double x) -> x
					+ randomNess * (rand.nextDouble() - .5));
			users.put(rating.getUserId(), userRandomd.add(deltaUser));
		}
		RealVector testMovieVector = movies.get(toTrain.get(0).getMovieId());
		System.out.println(testMovieVector);
		RealVector testUserVector = users.get(toTrain.get(0).getUserId());
		System.out.println(testUserVector);
		System.out.println("interaction: "
				+ toScale(sigmoid(testUserVector.dotProduct(testMovieVector))));

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
		return toScale(sigmoid(movie.dotProduct(user)));
	}

	private RealVector getMovieVector(Rating rating) {
		RealVector realVector = movies.get(rating.getMovieId());
		if (realVector == null) {
			realVector = MatrixUtils.createRealVector(new double[featureSize]);
			movies.put(rating.getMovieId(), realVector);
		}
		return realVector;
	}

	private RealVector getUserVector(Rating rating) {
		RealVector realVector = users.get(rating.getUserId());
		if (realVector == null) {
			realVector = MatrixUtils.createRealVector(new double[featureSize]);
			users.put(rating.getUserId(), realVector);
		}
		return realVector;
	}

}
