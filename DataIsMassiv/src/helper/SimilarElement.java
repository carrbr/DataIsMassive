package helper;

import java.io.Serializable;

public class SimilarElement implements Comparable<SimilarElement>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4013193921824094551L;
	public int id;
	public double similarity;

	public SimilarElement(int id, double similarity) {
		this.id = id;
		this.similarity = similarity;
	}

	@Override
	public int compareTo(SimilarElement user) {
		int result = 0;
		if (this.similarity > user.similarity) {
			result = 1;
		} else if (this.similarity < user.similarity) {
			result = -1;
		}
		return result;
	}
}
