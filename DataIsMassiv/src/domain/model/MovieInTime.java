package domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import domain.Rating;

public class MovieInTime implements DelatAccess, Serializable {
	private static final long serialVersionUID = 1L;

	private static class AVGPair implements Serializable {
		private static final long serialVersionUID = 4039626946427181487L;
		int count = 0;
		double totalScore = 0;

		public void add(double score) {
			count++;
			totalScore += score;
		}

		public double get() {
			return totalScore/count;
		}
	}

	private final HashMap<Integer, AVGPair> movieTime;

	public MovieInTime() {
		movieTime = new HashMap<>();
	}

	public void train(List<Rating> toTrain, BaseLearner base) {
		System.out.println("starting training on biasMovie");
		for (Rating r : toTrain) {

			AVGPair avg = movieTime.get(r.getMovieId());
			if (avg == null) {
				avg = new AVGPair();
				movieTime.put(r.getMovieId(), avg);
			}
			avg.add(base.getDelta(r)-r.getRating());
		}

	}


	@Override
	public double getDelta(Rating rating) {
		AVGPair avg = movieTime.get(rating.getMovieId());
		if (avg == null) {
			avg = new AVGPair();
			movieTime.put(rating.getMovieId(), avg);
		}
		return avg.get();
	}

}
