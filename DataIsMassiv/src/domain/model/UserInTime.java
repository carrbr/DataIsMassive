package domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

import domain.LearningCardNN;
import domain.Neural3LayerNetwork;
import domain.Neural3LayerNetwork.LearnResponse;
import domain.Rating;

public class UserInTime implements DelatAccess, Serializable {

	private static final long serialVersionUID = 1L;
	private static final int hh = 10;
	private static final int h = 5;

	private final HashMap<Integer, Neural3LayerNetwork> movieTime;

	public UserInTime() {
		movieTime = new HashMap<>();
	}

	public void train(List<Rating> toTrain, BaseLearner base) {
		System.out.println("starting training on biasUser(t)");
		for (Rating r : toTrain) {

			Neural3LayerNetwork network = movieTime.get(r.getUserId());
			if (network == null) {
				network = new Neural3LayerNetwork(TimeVector.timeVecDim, hh, h,
						1);
				movieTime.put(r.getUserId(), network);
			}

			teachNetwork(base, r, network);
		}

	}

	private void teachNetwork(BaseLearner base, Rating r,
			Neural3LayerNetwork network) {
		RealVector response = createResponse(base, r);
		LearnResponse lr = network.learn(
				TimeVector.createVectorOn(r.getDateId()), response);
		LearningCardNN howToLearn = new LearningCardNN();
		howToLearn.etaNN = .3;
		network.writeLayerUpdate(lr, howToLearn);
	}

	private RealVector createResponse(BaseLearner base, Rating r) {
		double v[] = new double[1];
		v[0] = toNN(r.getRating() - base.getDelta(r));
		RealVector response = MatrixUtils.createRealVector(v);
		return response;
	}
	
	private double toScale(double delta){
		return delta*4 -2;
	}
	private double toNN(double delta){
		return (delta+2)/4;
	}
	

	@Override
	public double getDelta(Rating rating) {
		Neural3LayerNetwork network = movieTime.get(rating.getMovieId());
		if (network == null)
			return 0;
		return toScale(network.respond(TimeVector.createVectorOn(rating.getDateId()))
				.getEntry(0));
	}

}
