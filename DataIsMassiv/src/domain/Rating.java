
package domain;

public class Rating {
	private int userId;
	private int movieId;
	private int dateId;
	private float rating;

	public Rating(int userId, int movieId, int dateId, float rating) {
		this.userId = userId;
		this.movieId = movieId;
		this.dateId = dateId;
		this.rating = rating;
	}

	public Rating(int userId, int movieId, int dateId) {
		this(userId, movieId, dateId, (short) 0);
	}

	
	public Rating reRate(float newRating){
		rating = newRating;
		return this;
//		return new Rating(userId, movieId, dateId, newRating);
	}
	
	@Override
	public String toString() {
		return userId + "," + movieId + "," + dateId + "," + getNiceFormatRating();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dateId;
		result = prime * result + movieId;
		result = prime * result + userId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Rating) {
			Rating that = (Rating) obj;
			
			if (userId != that.userId || movieId != that.movieId || dateId != that.dateId || rating != that.rating)
				return false;
			return true;
		}
		return false;
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		Rating other = (Rating) obj;
//		if (dateId != other.dateId)
//			return false;
//		if (movieId != other.movieId)
//			return false;
//		if (getRating() != other.getRating())
//			return false;
//		if (userId != other.userId)
//			return false;
//		return true;
	}

	public double getRating() {
		return rating;
	}
	
	public double getNiceFormatRating() {
		return (int)(rating*10)/(10.0);
	}
}
