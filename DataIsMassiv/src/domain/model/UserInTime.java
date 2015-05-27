package domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import domain.Rating;

public class UserInTime implements DelatAccess, Serializable {

	private static final long serialVersionUID = 1L;
	private final HashMap<Integer, AVGPair> movieTime;

	private static class AVGPair implements Serializable{
		private static final long serialVersionUID = -2861423457296709820L;
		int count = 0;
		double totalScore = 0;

		public void add(double score) {
			count++;
			totalScore += score;
		}

		public double get() {
			return totalScore / count;
		}
	}

	public UserInTime() {
		movieTime = new HashMap<>();
	}

	public void train(List<Rating> toTrain, BaseLearner base) {
		System.out.println("starting training on biasUser");
		for (Rating r : toTrain) {

			AVGPair avg = movieTime.get(r.getUserId());
			if (avg == null) {
				avg = new AVGPair();
				movieTime.put(r.getUserId(), avg);
			}
			avg.add(base.getDelta(r) - r.getRating());
		}

	}

	@Override
	public double getDelta(Rating rating) {
		AVGPair avg = movieTime.get(rating.getUserId());
		if (avg == null) {
			avg = new AVGPair();
			movieTime.put(rating.getUserId(), avg);
		}
		return avg.get();
	}
}
