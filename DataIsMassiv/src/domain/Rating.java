
package domain;

public class Rating {
	final private int userId;
	final private int movieId;
	final private int dateId;
	final private float rating;

	public Rating(int userId, int movieId, int dateId, float rating) {
		this.userId = userId;
		this.movieId = movieId;
		this.dateId = dateId;
		this.rating = rating;
	}

	public Rating(int userId, int movieId, int dateId) {
		this(userId, movieId, dateId, (short) 0);
	}

	
	public int getUserId() {
		return userId;
	}

	public int getMovieId() {
		return movieId;
	}

	public int getDateId() {
		return dateId;
	}

	public Rating reRate(float newRating){
		return new Rating(userId, movieId, dateId, newRating);
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
		result = prime * result + Float.floatToIntBits(rating);
		result = prime * result + userId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rating other = (Rating) obj;
		if (dateId != other.dateId)
			return false;
		if (movieId != other.movieId)
			return false;
		if (Float.floatToIntBits(rating) != Float.floatToIntBits(other.rating))
			return false;
		if (userId != other.userId)
			return false;
		return true;
	}

	public int getUserId() {
		return userId;
	}

	public int getMovieId() {
		return movieId;
	}

	public int getDateId() {
		return dateId;
	}

	public float getRating() {
		return rating;
	}
	
	public double getNiceFormatRating() {
		return (int)(rating*10)/(10.0);
	}
}
