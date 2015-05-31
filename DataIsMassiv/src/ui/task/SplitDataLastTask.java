package ui.task;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import domain.Rating;

public class SplitDataLastTask extends TaskCommand {

	private final static int lastOf = 150;
	private String fileInTrain;
	private String fileOut;

	public SplitDataLastTask(String[] args) {
		super(args);

		if (!needsHelp && args.length == 2) {
			this.fileInTrain = args[0];
			this.fileOut = args[1];
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;

		TextToRatingReader ratingsInTrain = null;
		BufferedWriter writerTraining = null;

		try {

			ratingsInTrain = new TextToRatingReader(fileInTrain);
			writerTraining = new BufferedWriter(new FileWriter(
					new File(fileOut)));

			ArrayList<Rating> r = readIn(ratingsInTrain);
			int lastDay = searchLast(r);
			copyOut(writerTraining, r, lastDay);

		} finally {
			if (writerTraining != null)
				writerTraining.close();
		}

	}

	private int searchLast(ArrayList<Rating> rl) {
		int max = 0;
		for (Rating r : rl) {
			max = r.getDateId() > max ? r.getDateId() : max;
		}
		return max;
	}

	private ArrayList<Rating> readIn(TextToRatingReader ratingsInTrain)
			throws IOException {
		ArrayList<Rating> al = new ArrayList<>();
		Rating r = null;
		while ((r = ratingsInTrain.readNext()) != null) {
			al.add(r);
		}
		return al;
	}

	private void copyOut(BufferedWriter writerTraining,
			ArrayList<Rating> outList, int lastDay) throws IOException {
		for (Rating r : outList) {
			if (lastDay - r.getDateId() <= lastOf)
				writerTraining.append(r + "\n");
		}

	}

}
