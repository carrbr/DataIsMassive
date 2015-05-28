package ui.task;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.Queue;

import domain.Rating;
import domain.SimilarityRating;

public class CombineModelTask extends TaskCommand {

	private String[] modelResultFiles;
	private String resultFile;
	private float weight;
	Queue<Rating> ratings;

	public CombineModelTask(String resultFile, String[] modelResultFiles) {
		this.modelResultFiles = modelResultFiles;
		this.resultFile = resultFile;
		this.weight = (float) (1.0 / modelResultFiles.length);
		this.ratings = new ArrayDeque<Rating>();
	}

	public CombineModelTask(String[] args) {
		super(args);

		if (!needsHelp && args.length >= 2) {
			this.resultFile = args[0];
			this.modelResultFiles = new String[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				this.modelResultFiles[i - 1] = args[i];
			}
			this.weight = (float) (1.0 / modelResultFiles.length);
			this.ratings = new ArrayDeque<Rating>();
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;

		SimilarityRating r = null;
		SimilarityRating rPrev = null;
		double rSim = -1.0;
		TextToRatingReader in = null;
		BufferedWriter out = null;

		for (int i = 0; i < modelResultFiles.length; i++) {
			try {
				in = new TextToRatingReader(modelResultFiles[i]);
				while ((r = (SimilarityRating)in.readNext()) != null) {
					rSim = r.getSimilarity();
					if (r.getRating() > 5.0 || r.getRating() < 1.0) {
						System.out.println("Halp rating = " + r.getRating() + " uId = " + r.getUserId() + " mId = " + r.getMovieId());
					}
					if (i != 0) {
						rPrev = (SimilarityRating)ratings.poll();
						ratings.add(rPrev.addToRating(rSim, (float)rSim * r.getRating()));
					} else { // first iteration, need to fill the queue
						ratings.add(r.addToRating(rSim, (float)rSim * r.getRating()));
					}
				}
	
			} finally {
				if (in != null)
					in.close();
			}
		}
		try {
			out = new BufferedWriter(new FileWriter(new File(resultFile)));
			Rating rate = null;
			while (ratings.peek() != null) {
				r = (SimilarityRating) ratings.poll();
				rSim = r.getSimilarity();
				if (rSim == 0.0) { // no one has a clue... just return the average
					rate = r.reRate((float) (r.getRating() / modelResultFiles.length));
				} else {
					rate = r.reRate((float) (r.getRating() / r.getSimilarity()));
				}
				if (rate.getRating() > 5.0 || rate.getRating() < 1.0) {
					System.out.println("Halp");
				}
				out.write(rate.toString() + "\n");
			}
		} finally {
			if (out != null)
				out.close();
		}

	}
}
