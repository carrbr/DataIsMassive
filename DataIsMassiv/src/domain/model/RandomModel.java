
package domain.model;

import java.util.Random;

import domain.Rating;

public class RandomModel extends AbstractRatingModel {
	private static final long serialVersionUID = 6965776703302366162L;
	volatile Random random = new Random(0xFAC4C0FE);
	
	@Override
	public Rating predict(Rating r) {
		return r.reRate(((float)random.nextInt(41) + 10)/10);
	}
}
