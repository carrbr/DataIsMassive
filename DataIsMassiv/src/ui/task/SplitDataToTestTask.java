package ui.task;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import domain.Rating;

public class SplitDataToTestTask extends TaskCommand {

	private final static int daysPerBucket = 20;
	private String fileInTrain;
	private String fileInTest;
	private String fileOut;
	private int numOfTestCases;

	public SplitDataToTestTask(String fileInTrain, String fileInTest,
			String fileOut, int numOfTestCases) {
		this.fileInTrain = fileInTrain;
		this.fileInTest = fileInTest;
		this.fileOut = fileOut;
		this.numOfTestCases = numOfTestCases;

	}

	public SplitDataToTestTask(String[] args) {
		super(args);

		if (!needsHelp && args.length == 4) {
			this.fileInTrain = args[0];
			this.fileInTest = args[1];
			this.fileOut = args[2];
			this.numOfTestCases = Integer.valueOf(args[3]);
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;

		TextToRatingReader ratingsIn = null;
		TextToRatingReader ratingsInTrain = null;
		BufferedWriter writerTraining = null;

		try {
			ratingsIn = new TextToRatingReader(fileInTest);
			ratingsInTrain = new TextToRatingReader(fileInTrain);
			writerTraining = new BufferedWriter(new FileWriter(
					new File(fileOut)));

			List<Rating> inTest = readInToList(ratingsIn);
			TreeMap<Integer, Integer> histographTest = generateInputHistograph(inTest);
			int totalTest = countTotal(histographTest);

			List<Rating> inTrain = readInToList(ratingsInTrain);
			TreeMap<Integer, Integer> histographTrain = generateInputHistograph(inTrain);

			Random rand = new Random();
			ArrayList<Rating> outList = new ArrayList<Rating>(numOfTestCases);
			System.out.println("starting filtering");
			while (outList.size() < numOfTestCases) {
				Rating r = inTrain.get(rand.nextInt(inTrain.size()));
				double inverseLikelyhoodOfData = 1.0 / histographTrain.get(r
						.getDateId() / daysPerBucket);
				if (inverseLikelyhoodOfData > rand.nextDouble())
					continue;

				Integer countlocalImportantness = histographTest.get(r
						.getDateId() / daysPerBucket);
				double localImportantness = countlocalImportantness == null ? 1
						: countlocalImportantness / ((double) totalTest);
				if (localImportantness < rand.nextDouble())
					continue;

				outList.add(r);

			}

			copyOut(writerTraining, outList);

		} finally {
			if (ratingsIn != null)
				ratingsIn.close();
			if (writerTraining != null)
				writerTraining.close();
		}

	}

	private void copyOut(BufferedWriter writerTraining,
			ArrayList<Rating> outList) throws IOException {
		for (Rating r : outList) {
			writerTraining.append(r + "\n");
		}

	}

	private List<Rating> readInToList(TextToRatingReader ratingsIn)
			throws IOException {
		List<Rating> list = new ArrayList<Rating>(5000);
		Rating r = null;
		while ((r = ratingsIn.readNext()) != null) {
			list.add(r);
		}
		return list;
	}

	private int countTotal(TreeMap<Integer, Integer> histograph) {
		int count = 0;
		for (Integer i : histograph.values()) {
			count += i;
		}
		return count;
	}

	private TreeMap<Integer, Integer> generateInputHistograph(
			List<Rating> ratingsIn) {
		TreeMap<Integer, Integer> histograph = new TreeMap<>();
		for (Rating r : ratingsIn) {
			int bucketNo = r.getDateId() / daysPerBucket;
			Integer count = histograph.get(bucketNo);
			if (count == null) {
				histograph.put(bucketNo, 1);
			} else {
				histograph.put(bucketNo, count + 1);
			}

		}
		return histograph;
	}
}
