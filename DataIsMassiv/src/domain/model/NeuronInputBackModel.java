package domain.model;

import helper.Neural3LayerNetwork;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import domain.Rating;

public class NeuronInputBackModel extends AbstractRatingModel implements
		Serializable {
	private static final long serialVersionUID = 1L;
	private final Neural3LayerNetwork nn;
	private final HashMap<Integer, RealVector> user;
	private final HashMap<Integer, RealVector> movies;
	private final int vecTime;
	private final int vecUser;
	private final int vecMovies;

	/**
	 * a unknown User will have the following 'initial' interest. The networks
	 * job is to recognize a new guy
	 */
	private final double[] newUserVec;
	private final double[] newMovieVec;

	public NeuronInputBackModel() {
		vecTime = 30;
		vecUser = 100;
		vecMovies = 100;
		int inputCount = vecTime + vecUser + vecMovies;
		nn = new Neural3LayerNetwork(inputCount, inputCount / 2, 30, 1);
		user = new HashMap<>();
		movies = new HashMap<>();
		newUserVec = new double[vecUser];
		newMovieVec = new double[vecMovies];
		generateNewVec();
	}

	private void generateNewVec() {
		Random r = new Random();
		for (int i = 0; i < vecUser; i++) {
			newUserVec[i] = r.nextDouble() - .5;
		}

		for (int i = 0; i < vecMovies; i++) {
			newMovieVec[i] = r.nextDouble() - .5;
		}

	}

	private RealVector getMovieVector(int movieID) {
		RealVector v = movies.get(movieID);
		if (v == null) {
			v = MatrixUtils.createRealVector(newUserVec);
			movies.put(movieID, v);
		}
		return v;
	}

	private RealVector getUserVector(int usrID) {
		RealVector v = user.get(usrID);
		if (v == null) {
			v = MatrixUtils.createRealVector(newUserVec);
			user.put(usrID, v);
		}
		return v;
	}

	private RealVector generateTimeVector(int time) {
		// First in 4 is frequeny, other 3 are phase translation
		double[][] para = { { 4984, 0, 1.1, 2.5 }, { 3562.43, .25, 1.6, 2.1 },
				{ 1460.97, .5, 1.8, 3.3 }, { 365.243, .2, 1.35, 2.6 },
				{ 91.31, .5, 1.7, 2.4 }, { 29.6, 0, .9, 1.6 },
				{ 10, .4, 1.6, 3 }, { 7, .2, .7, 2.1 }, { 4, .3, 1.7, 2.2 },
				{ 3, .1, 1.5, 2.9 } };
		double[] data = new double[30];
		for (int i = 0; i < 10; i++) {
			double localPhase = time / para[i][0];
			data[i * 3 + 0] = Math.sin(localPhase + para[i][1]) / 2 + .5;
			data[i * 3 + 1] = Math.sin(localPhase + para[i][2]) / 2 + .5;
			data[i * 3 + 2] = Math.sin(localPhase + para[i][3]) / 2 + .5;
		}
		return MatrixUtils.createRealVector(data);
	}

	private double predictionRescaleNetworkToMovieScale(double netScala) {
		return ((int) (netScala * 40 + 10)) / 10.0;
	}

	@Override
	public Rating predict(Rating r) {
		// TODO Auto-generated method stub
		return null;
	}

}
