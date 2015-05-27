package domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import domain.Rating;

public class Interaction implements DelatAccess, Serializable {
	private static final long serialVersionUID = 1L;
	private static double eta = .2;
	private final int featureSize = 30;
	private HashMap<Integer, RealVector> movies = new HashMap<>();
	private HashMap<Integer, RealVector> users = new HashMap<>();

	public void train(List<Rating> toTrain, BaseLearner base,
			MovieInTime movieTime, UserInTime userTime) {

		System.out.println("starting training on interaction");
		for (Rating rating : toTrain) {
			double teacher = rating.getRating() - base.getDelta(rating)
					- movieTime.getDelta(rating) - userTime.getDelta(rating);
			teacher = toSigmoid(teacher);

			double student = toSigmoid(getDelta(rating));

			double error = teacher - student;

			double correctionFactor = sigmoDiff(student) * error;
			RealVector movieVector = getMovieVector(rating);
			RealVector deltaMovie = movieVector.mapMultiply(correctionFactor
					* eta);
			movies.put(rating.getMovieId(), movieVector.add(deltaMovie));

			RealVector userVector = getUserVector(rating);
			RealVector deltaUser = userVector.mapMultiply(correctionFactor
					* eta);
			users.put(rating.getUserId(), userVector.add(deltaUser));
		}

	}

	private static double sigmoid(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	private static double toScale(double sigmoidOut) {
		return sigmoidOut * 8 - 4;
	}

	private static double toSigmoid(double scale) {
		return (scale + 4) / 8;
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
