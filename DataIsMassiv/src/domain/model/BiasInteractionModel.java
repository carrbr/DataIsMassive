package domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import domain.DataMonitor;
import domain.Rating;
import domain.model.Interaction.LearningSpecs;

public class BiasInteractionModel extends AbstractRatingModel implements
		DelatAccess, Serializable {

	private static final long serialVersionUID = 1L;
	private final BaseLearner base;
	private final MovieInTime movie;
	private final UserInTime user;
	private final Interaction interaction;
	private transient DataMonitor monitor = null;

	public BiasInteractionModel(int featureSize) {
		base = new BaseLearner();
		movie = new MovieInTime();
		user = new UserInTime();
		interaction = new Interaction(818, 5000, featureSize);
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
		LearningSpecs specs = new LearningSpecs();
		specs.etaMovie = .7;
		specs.etaUser = .15;
		specs.lambda = .0001;
		specs.randomNess = .04;
		interaction.train(toTrain, base, movie, user, specs);
	}

	public void trainNN(List<Rating> toTrain, double heat) {

		LearningSpecs specsOnHeat = genereateSpecsOnHeat(heat);

		ArrayList<Rating> rlist = shuffleNewTraingSet(toTrain);
		interaction.train(rlist, base, movie, user, specsOnHeat);

	}

	private ArrayList<Rating> shuffleNewTraingSet(List<Rating> toTrain) {
		ArrayList<Rating> rlist = new ArrayList<>(toTrain.size());
		Random rand = new Random();
		while (rlist.size() < toTrain.size())
			rlist.add(toTrain.get(rand.nextInt(toTrain.size())));
		return rlist;
	}

	private LearningSpecs genereateSpecsOnHeat(double heat) {
		LearningSpecs specs = new LearningSpecs();
		specs.etaMovie = .01 + heat * .05;
		specs.etaUser = .05 + heat * .1;
		specs.randomNess = 0 + heat * 0.00001;
		return specs;
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
		double d = baseP + movieP + userP + interactionPrediction;

		if (monitor != null && rating.getRating() >= 1) {
			monitor.reportInteraction(rating.getDateId(),
					interactionPrediction, rating.getRating() - baseP - movieP
							- userP);
		}

		return bound(d);
	}

	public void setMonitor(DataMonitor monitor) {
		this.monitor = monitor;
	}

}
