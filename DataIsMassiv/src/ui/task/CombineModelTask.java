package ui.task;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import domain.Rating;
import domain.SimilarityRating;

public class CombineModelTask extends TaskCommand {

	private String[] modelResultFiles;
	private String resultFile;
	private boolean useSim;
	private float weight;
	ArrayList<Rating> ratings;
	Queue<Rating> potentialTroubleRatings;

	public CombineModelTask(String resultFile, boolean sim, String[] modelResultFiles) {
		this.modelResultFiles = modelResultFiles;
		this.resultFile = resultFile;
		this.useSim = sim;
		this.weight = (float) (1.0 / modelResultFiles.length);
		this.ratings = new ArrayList<Rating>();
		this.potentialTroubleRatings = new ArrayDeque<Rating>();

	}

	public CombineModelTask(String[] args) {
		super(args);

		if (!needsHelp && args.length >= 3) {
			if (args[0].equals("sim")) {
				useSim = true;
			} else if (!args[0].equals("nosim")) {
				System.err.println("bad arg in combine task");
			} else {
				useSim = false;
			}
			this.resultFile = args[1];
			this.modelResultFiles = new String[args.length - 2];
			for (int i = 2; i < args.length; i++) {
				this.modelResultFiles[i - 2] = args[i];
			}
			this.weight = (float) (1.0 / modelResultFiles.length);
			this.ratings = new ArrayList<Rating>();
			this.potentialTroubleRatings = new ArrayDeque<Rating>();
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;
		try {
		if (useSim) {
			combineUsingSim();
		} else {
			combineNoSim();
		}
		} catch (IOException e) {
			
		}
	}
	
	private void combineUsingSim() throws IOException {
		SimilarityRating r = null;
		SimilarityRating rPrev = null;
		double rSim = -1.0;
		TextToRatingReader in = null;
		BufferedWriter out = null;

		for (int i = 0; i < modelResultFiles.length; i++) {
			try {
				int count = 0;
				in = new TextToRatingReader(modelResultFiles[i]);
				while ((r = (SimilarityRating) in.readNext()) != null) {
					rSim = r.getSimilarity();
					if (i != 0) {
						rPrev = (SimilarityRating) ratings.get(count);
						ratings.set(count, rPrev.addToRating(rSim, (float) rSim * r.getRating()));
						count++;
					} else { // first iteration, need to fill the queue
						if (r.getSimilarity() == 0.0) {
							potentialTroubleRatings.add(r);
						}
						ratings.add(ratings.size(), new SimilarityRating(rSim, r.getUserId(), r
								.getMovieId(), r.getDateId(), (float) rSim * r.getRating()));
					}
				}

			} finally {
				if (in != null)
					try {
						in.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}
		try {
			out = new BufferedWriter(new FileWriter(new File(resultFile)));
			Rating rate = null;
			SimilarityRating sr = null;
			for(int i = 0; i < ratings.size(); i++) {
				sr = (SimilarityRating) ratings.get(i);
				rSim = sr.getSimilarity();
				if (rSim == 0.0) { // no one has a clue... just return the average
					rate = findRating(sr);
				} else {
					rate = sr.reRate((float) (sr.getRating() / sr.getSimilarity()));
				}
				out.write(rate.toString() + "\n");
			}
		} finally {
			if (out != null)
				out.close();
		}
	}
	
	private void combineNoSim() throws IOException {
		Rating r = null;
		Rating rPrev = null;
		TextToRatingReader in = null;
		BufferedWriter out = null;

		for (int i = 0; i < modelResultFiles.length; i++) {
			try {
				int count = 0;
				in = new TextToRatingReader(modelResultFiles[i]);
				while ((r = in.readNext()) != null) {
					if (i != 0) {
						rPrev = ratings.get(count);
						ratings.set(count, rPrev.reRate(weight * r.getRating() + rPrev.getRating()));
						count++;
					} else { // first iteration, need to fill the queue
						ratings.add(ratings.size(), new Rating(r.getUserId(), r
								.getMovieId(), r.getDateId(), weight * r.getRating()));
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
			Rating sr = null;
			for(int i = 0; i < ratings.size(); i++) {
				sr = ratings.get(i);
				rate = sr.reRate((float) (sr.getRating()));
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
			if (s.getUserId() != r.getUserId()
					|| s.getMovieId() != r.getMovieId()
					|| s.getDateId() != r.getDateId()) {
				potentialTroubleRatings.add(s);
			} else {
				potentialTroubleRatings.add(s);
				break;
			}
			i--;
		} while (i > 0);
		return s;
	}
}
