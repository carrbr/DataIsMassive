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
	private int mod;
	private boolean splitByUser;

	public SplitDataTask(String fileIn, String fileOut, int numOfBuckets, int mod, boolean splitByUser) {
		this.fileIn = fileIn;
		this.fileOut = fileOut;
		this.numOfBuckets = numOfBuckets;
		this.mod = mod;
		this.splitByUser = splitByUser;

	}

	public SplitDataTask(String[] args) {
		super(args);

		if (!needsHelp && args.length >= 4) {
			this.fileIn = args[0];
			this.fileOut = args[1];
			this.numOfBuckets = Integer.valueOf(args[2]);
			this.numOfBuckets = Integer.valueOf(args[3]);
			if (args.length >= 5 && args[4].equals("usplit")) {
				this.splitByUser = true;
			} else {
				this.splitByUser = false;
			}
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

			if (this.splitByUser) {
				divideByUser(ratingsIn, writerTraining, writerTest, numOfBuckets, mod);
			} else {
				divide(ratingsIn, writerTraining, writerTest, numOfBuckets, mod);
			}
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
			int numOfBuckets, int mod) throws IOException {

		Rating r = null;
		while ((r = ratingsIn.readNext()) != null) {
			int h = r.hashCode();
			h %= numOfBuckets;
			if (h == mod) {
				writerTest.write(r + "\n");
			} else {
				writerTraining.write(r + "\n");
			}
		}

	}

	private void divideByUser(TextToRatingReader ratingsIn,
			BufferedWriter writerTraining, BufferedWriter writerTest,
			int numOfBuckets, int mod) throws IOException {

		Rating r = null;
		while ((r = ratingsIn.readNext()) != null) {
			int h = r.getUserId();
			h %= numOfBuckets;
			if (h == mod) {
				writerTest.write(r + "\n");
			} else {
				writerTraining.write(r + "\n");
			}
		}

	}

}
