package domain;

public class Rating {
	private int userId;
	private int movieId;
	private int dateId;
	private short rating;

	public Rating(int userId, int movieId, int dateId, short rating) {
		this.userId = userId;
		this.movieId = movieId;
		this.dateId = dateId;
		this.rating = rating;
	}

	public Rating(int userId, int movieId, int dateId) {
		this(userId, movieId, dateId, (short) 0);
	}

	
	public Rating reRate(short newRating){
		return new Rating(userId, movieId, dateId, newRating);
	}
	
	@Override
	public String toString() {
		return userId + "," + movieId + "," + dateId + "," + getRating();
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
		if (getRating() != other.getRating())
			return false;
		if (userId != other.userId)
			return false;
		return true;
	}

	public short getRating() {
		return rating;
	}

}
