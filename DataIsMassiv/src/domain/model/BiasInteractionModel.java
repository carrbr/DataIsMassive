package domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import domain.DataMonitor;
import domain.Rating;

public class BiasInteractionModel extends AbstractRatingModel implements
		DelatAccess, Serializable {

	private static final long serialVersionUID = 1L;
	private final BaseLearner base;
	private final MovieInTime movie;
	private final UserInTime user;
	private final Interaction interaction;
	private transient DataMonitor monitor = null;

	public BiasInteractionModel() {
		base = new BaseLearner();
		movie = new MovieInTime();
		user = new UserInTime();
		interaction = new Interaction(818, 5000);
	}

	@Override
	public Rating predict(Rating r) {
		return r.reRate((float) getDelta(r));
	}

	public void train(List<Rating> toTrain) {
		System.out.println("Calculate avg:");
		base.train(toTrain);
		System.out.println("Calculate Bias movie(t):");
		movie.train(toTrain, base);
		System.out.println("Calculate Bias user(t):");
		user.train(toTrain, base);
		interaction.train(toTrain, base, movie, user, .05);
	}

	public void trainNN(List<Rating> toTrain) {
		for (int i = 0; i < 10; i++) {
			ArrayList<Rating> rlist = new ArrayList<>(toTrain.size());
			Random rand = new Random();
			while (rlist.size() < toTrain.size())
				rlist.add(toTrain.get(rand.nextInt(toTrain.size())));
			interaction.train(rlist, base, movie, user, 0.00001);

		}

	}

	private double bound(double d) {
		return Math.max(1, Math.min(5, d));
	}

	@Override
	public double getDelta(Rating rating) {
		double interactionPrediction = interaction.getDelta(rating);
		double baseP = base.getDelta(rating);
		double movieP = movie.getDelta(rating);
		double userP = user.getDelta(rating);
		double d = baseP + movieP + userP +  interactionPrediction;

		if (monitor != null && rating.getRating() >= 1) {
			monitor.reportInteraction(rating.getDateId(),
					interactionPrediction, rating.getRating()-baseP -movieP- userP);
		}

		return bound(d);
	}

	public void setMonitor(DataMonitor monitor) {
		this.monitor = monitor;
	}

}
