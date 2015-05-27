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
	private static double etaUser = .1;
	private final int featureSize = 8;
	private HashMap<Integer, RealVector> movies = new HashMap<>();
	private HashMap<Integer, RealVector> users = new HashMap<>();

	public void train(List<Rating> toTrain, BaseLearner base,
			MovieInTime movieTime, UserInTime userTime) {
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

			RealVector userStepIntensity = userVector.map((double x) -> {
				if (x >= 0)
					return x * x + 1 + (rand.nextDouble() - 0.5);
				else
					return -(x * x + 1 + (rand.nextDouble() - 0.5));
			});
			RealVector deltaMovie = userStepIntensity.mapMultiply(gradientDelta
					* etaMovie / featureSize);
			movies.put(rating.getMovieId(), movieVector.add(deltaMovie));

			RealVector movieStepIntensity = movieVector.map((double x) -> {
				if (x >= 0)
					return x * x + 1 + (rand.nextDouble() - 0.5);
				else
					return -(x * x + 1 + (rand.nextDouble() - 0.5));
			});
			RealVector deltaUser = movieStepIntensity.mapMultiply(gradientDelta
					* etaUser / featureSize);
			users.put(rating.getUserId(), userVector.add(deltaUser));
		}

	}

	private static double sigmoid(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	private static double toScale(double sigmoidOut) {
		return sigmoidOut * 10 - 5;
	}

	private static double toSigmoid(double scale) {
		return (scale + 5) / 10;
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
			movies.put(rating.getUserId(), realVector);
		}
		return realVector;
	}

}
