package helper;

import java.io.Serializable;

public abstract class AbstractSimilarUser implements Comparable<AbstractSimilarUser>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4407308927380143463L;

	public int id;
	public double similarity;
	
	AbstractSimilarUser(int id, double similarity) {
		this.id = id;
		this.similarity = similarity;
	}
}
