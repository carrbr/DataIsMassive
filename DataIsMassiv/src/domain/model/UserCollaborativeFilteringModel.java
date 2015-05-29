package domain.model;

import helper.AbstractRatingSet;
import helper.UserRatingSet;

public class UserCollaborativeFilteringModel extends
		AbstractCollaborativeFilteringModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5344448264711157886L;

	public UserCollaborativeFilteringModel(String trainingSetFile, int n,
			double minSim, int minCount, double pcWeight) {
		super(trainingSetFile, n, minSim, minCount, pcWeight);
	}

	/*
	 * Abstract method implementations
	 */

	@Override
	protected AbstractRatingSet generateRatingSet() {
		return new UserRatingSet();
	}

	@Override
	protected String getLogFileName() {
		return "UCF.log";
	}
}
