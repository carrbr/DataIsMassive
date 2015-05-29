package domain.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import domain.AVGPair;
import domain.Rating;

public class UserInTime implements DelatAccess, Serializable {

	private static final long serialVersionUID = 1L;
	private final HashMap<Integer, AVGPair> movieTime;



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
		return avg.getAVG();
	}
}
