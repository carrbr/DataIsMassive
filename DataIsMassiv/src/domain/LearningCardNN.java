package domain;

public class LearningCardNN {
	/**
	 * learning rates
	 */
	public double etaNN = 0;
	public double etaMovie = 0;
	public double etaUser = 0;
	
	/**
	 * clear knowledge about Movies/Users
	 * used to relearn all inputs 
	 */
	public boolean resetMovie = false;
	public boolean resetUser = false;
	
	/**
	 * rate to reset knowledge to newVector
	 * used to let NN not forget how to handle new Users/Movies
	 * used to let Input not drift to far off
	 */
	public double resetRateMovie = 0;
	public double resetRateUser = 0;
	
	/**
	 * used only to train NN not to forget
	 */
	public double fakeInput = 0;
}
