package ui;

import domain.Rating;
import helper.TextToRatingReader;

public class RMSETask implements TaskCommand {

	private String shouldFile;
	private String isFile;

	public RMSETask(String shouldFile, String isFile) {
		this.shouldFile = shouldFile;
		this.isFile = isFile;

	}

	@Override
	public void exec() throws Exception {
		TextToRatingReader should = null;
		TextToRatingReader is = null;

		try {
			should = new TextToRatingReader(shouldFile);
			is = new TextToRatingReader(isFile);

			Rating shouldRating = null;

			long err = 0;
			long n = 0;
			while ((shouldRating = should.readNext()) != null) {
				Rating isRating = is.readNext();
				err += squareError(isRating, shouldRating);
				n++;
			}
			System.out.println("The RMSE is: " + Math.sqrt((double) err / n));
		} finally {
			if (should != null)
				should.close();
			if (is != null)
				is.close();
		}

	}

	private int squareError(Rating isRating, Rating shouldRating) {
		int i = isRating.getRating() - shouldRating.getRating();
		return i * i;
	}

}
