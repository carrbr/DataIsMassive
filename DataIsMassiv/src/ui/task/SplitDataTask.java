package ui.task;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import domain.Rating;

public class SplitDataTask extends TaskCommand {

	private String fileIn;
	private String fileOut;
	private int numOfBuckets;

	public SplitDataTask(String fileIn, String fileOut, int numOfBuckets) {
		this.fileIn = fileIn;
		this.fileOut = fileOut;
		this.numOfBuckets = numOfBuckets;

	}

	public SplitDataTask(String[] args) {
		super(args);

		if (!needsHelp && args.length == 3) {
			this.fileIn = args[0];
			this.fileOut = args[1];
			this.numOfBuckets = Integer.valueOf(args[2]);
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;

		TextToRatingReader ratingsIn = null;
		BufferedWriter writerTraining = null;
		BufferedWriter writerTest = null;
		try {
			ratingsIn = new TextToRatingReader(fileIn);
			writerTraining = new BufferedWriter(new FileWriter(new File(fileOut
					+ "_train")));
			writerTest = new BufferedWriter(new FileWriter(new File(fileOut
					+ "_test")));

			divide(ratingsIn, writerTraining, writerTest, numOfBuckets);

		} finally {
			if (ratingsIn != null)
				ratingsIn.close();
			if (writerTest != null)
				writerTest.close();
			if (writerTraining != null)
				writerTraining.close();
		}

	}

	private void divide(TextToRatingReader ratingsIn,
			BufferedWriter writerTraining, BufferedWriter writerTest,
			int numOfBuckets) throws IOException {

		Rating r = null;
		while ((r = ratingsIn.readNext()) != null) {
			int h = r.hashCode();
			h %= numOfBuckets;
			if (h == 0) {
				writerTest.write(r + "\n");
			} else {
				writerTraining.write(r + "\n");
			}
		}

	}
}
