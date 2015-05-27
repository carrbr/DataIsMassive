package domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import domain.Rating;

public class BiasInteractionModel extends AbstractRatingModel implements
		DelatAccess, Serializable {

	private static final long serialVersionUID = 1L;
	private final BaseLearner base;
	private final MovieInTime movie;
	private final UserInTime user;
	private final Interaction interaction;

	public BiasInteractionModel() {
		base = new BaseLearner();
		movie = new MovieInTime();
		user = new UserInTime();
		interaction = new Interaction();
	}

	@Override
	public Rating predict(Rating r) {
		return r.reRate((float) getDelta(r));
	}

	public void train(List<Rating> toTrain) {
		base.train(toTrain);
		movie.train(toTrain, base);
		user.train(toTrain, base);
		interaction.train(toTrain, base, movie, user);
	}

	public void trainNN(List<Rating> toTrain) {
		for (int i = 0; i < 10; i++) {
			ArrayList<Rating> rlist = new ArrayList<>(toTrain.size());
			Random rand = new Random();
			while (rlist.size() < toTrain.size())
				rlist.add(toTrain.get(rand.nextInt(toTrain.size())));
			interaction.train(toTrain, base, movie, user);

		}

	}

	@Override
	public double getDelta(Rating rating) {
		double d = base.getDelta(rating) + movie.getDelta(rating)
				+ user.getDelta(rating) + interaction.getDelta(rating);
		return bound(d);
	}

	private double bound(double d) {
		return Math.max(1, Math.min(5, d));
	}

}
