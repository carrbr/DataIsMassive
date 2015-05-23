package helper;

public class SimilarUser extends AbstractSimilarUser {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4013193921824094551L;

	public SimilarUser(int id, double similarity) {
		super(id, similarity);
	}

	@Override
	public int compareTo(AbstractSimilarUser user) {
		int result = 0;
		if (this.similarity > user.similarity) {
			result = 1;
		} else if (this.similarity > user.similarity) {
			result = -1;
		}
		return result;
	}
}
