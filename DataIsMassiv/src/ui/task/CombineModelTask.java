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
	Queue<Rating> potentialTroubleRatings;

	public CombineModelTask(String resultFile, String[] modelResultFiles) {
		this.modelResultFiles = modelResultFiles;
		this.resultFile = resultFile;
		this.weight = (float) (1.0 / modelResultFiles.length);
		this.ratings = new ArrayDeque<Rating>();
		this.potentialTroubleRatings = new ArrayDeque<Rating>();

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
			this.potentialTroubleRatings = new ArrayDeque<Rating>();

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
					if (i != 0) {
						rPrev = (SimilarityRating)ratings.poll();
						ratings.add(rPrev.addToRating(rSim, (float)rSim * r.getRating()));
					} else { // first iteration, need to fill the queue
						if (r.getSimilarity() == 0.0) {
							potentialTroubleRatings.add(r);
						}
						ratings.add(new SimilarityRating(rSim, r.getUserId(), r.getMovieId(), r.getDateId(), (float)rSim * r.getRating()));
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
					rate = findRating(r); //r.reRate((float) (r.getRating() / modelResultFiles.length));
				} else {
					rate = r.reRate((float) (r.getRating() / r.getSimilarity()));
				}
				out.write(rate.toString() + "\n");
			}
		} finally {
			if (out != null)
				out.close();
		}

	}
	
	private Rating findRating(Rating r) {
		Rating s = null;
		int i = potentialTroubleRatings.size();
		do {
			s = potentialTroubleRatings.poll();
			if (s.getUserId() != r.getUserId() || s.getMovieId() != r.getMovieId() || s.getDateId() != r.getDateId()) {
				potentialTroubleRatings.add(s);
			} else {
				break;
			}
			i--;
		} while (i > 0);
		return s;
	}
}
