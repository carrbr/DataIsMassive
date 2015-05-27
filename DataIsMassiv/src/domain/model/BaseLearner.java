package domain.model;

import java.io.Serializable;
import java.util.List;

import domain.Rating;

public class BaseLearner implements DelatAccess, Serializable {
	
	private static final long serialVersionUID = 1L;
	public double overAll;

	public void train(List<Rating> toTrain) {
		double c = 0;
		for(Rating r : toTrain){
			c += r.getRating();
		}
		overAll = c/toTrain.size();
		System.out.println("avg is "+ overAll);
	}

	@Override
	public double getDelta(Rating rating) {
		return overAll;
	}

}
