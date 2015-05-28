package domain;


public class SimilarityRating extends Rating {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8841141810216078918L;
	private double similarity;
	
	public SimilarityRating(double similarity, int userId, int movieId, int dateId, float rating) {
		super(userId, movieId, dateId, rating);
		this.similarity = similarity;
	}
	
	public Rating addToRating(double sim, float r) {
		return new SimilarityRating(similarity + sim, getUserId(), getMovieId(), getDateId(), getRating() + r);
	}
	
	public double getSimilarity() {
		return this.similarity;
	}
}
