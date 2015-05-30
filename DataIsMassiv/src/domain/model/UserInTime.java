package domain.model;

import java.io.Serializable;

import domain.Rating;

public class UserInTime extends BiasInTime implements DelatAccess, Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	protected int getItemId(Rating rating) {
		return rating.getUserId();
	}

}
