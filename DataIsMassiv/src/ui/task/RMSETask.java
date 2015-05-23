package ui.task;

import java.io.IOException;

import domain.Rating;
import helper.TextToRatingReader;

public class RMSETask extends TaskCommand {

	private String shouldFile;
	private String isFile;

	public RMSETask(String shouldFile, String isFile) {
		this.shouldFile = shouldFile;
		this.isFile = isFile;

	}

	public RMSETask(String[] args) {
		super(args);

		if (!needsHelp && args.length == 2) {
			this.shouldFile = args[0];
			this.isFile = args[1];
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;

		TextToRatingReader should = null;
		TextToRatingReader is = null;

		try {
			should = new TextToRatingReader(shouldFile);
			is = new TextToRatingReader(isFile);

			double rmse = calculateError(should, is);
			System.out.println("The RMSE is: " + rmse);
		} finally {
			if (should != null)
				should.close();
			if (is != null)
				is.close();
		}

	}

	private double calculateError(TextToRatingReader should,
			TextToRatingReader is) throws IOException {
		Rating shouldRating = null;
		double err = 0;
		long n = 0;
		while ((shouldRating = should.readNext()) != null) {
			Rating isRating = is.readNext();
			err += squareError(isRating, shouldRating);
			n++;
		}
		return Math.sqrt(err / n);
	}

	private double squareError(Rating isRating, Rating shouldRating) {
		double i = isRating.getRating() - shouldRating.getRating();
		return i * i;
	}

}