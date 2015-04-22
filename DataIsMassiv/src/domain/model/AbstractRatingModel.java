package domain.model;

import java.io.Serializable;

import domain.Rating;

public abstract class AbstractRatingModel implements Serializable {
	private static final long serialVersionUID = 1L;

	public abstract Rating predict(Rating r);

}