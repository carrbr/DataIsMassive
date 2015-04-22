package ui;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import domain.Rating;

public class SplitDataTask implements TaskCommand {

	private String fileIn;
	private String fileOut;
	private int numOfBuckets;

	public SplitDataTask(String fileIn, String fileOut, int numOfBuckets) {
		this.fileIn = fileIn;
		this.fileOut = fileOut;
		this.numOfBuckets = numOfBuckets;

	}

	@Override
	public void exec() throws Exception {
		TextToRatingReader ratingsIn = null;
		BufferedWriter writerTraining = null;
		BufferedWriter writerTest = null;
		try {
			ratingsIn = new TextToRatingReader(fileIn);
			writerTraining = new BufferedWriter(new FileWriter(
					new File(fileOut+"_train")));
			writerTest = new BufferedWriter(new FileWriter(new File(fileOut+"_test")));

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
				writerTraining.write(r+"\n");
			}
		}

	}
}
