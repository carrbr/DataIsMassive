package ui.task;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayDeque;
import java.util.Queue;

import domain.Rating;
import domain.model.AbstractRatingModel;

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


		TextToRatingReader in = null;
		BufferedWriter out = null;

		for (int i = 0; i < modelResultFiles.length; i++) {
			try {
				in = new TextToRatingReader(modelResultFiles[i]);
				Rating r = null;
				Rating rPrev = null;
				while ((r = in.readNext()) != null) {
					if (i != 0) {
						rPrev = ratings.poll();
						ratings.add(r.reRate(r.getRating() * weight + rPrev.getRating()));
					} else { // first iteration, need to fill the queue
						ratings.add(r.reRate(r.getRating() * weight));
					}
				}
	
			} finally {
				if (in != null)
					in.close();
			}
		}
		try {
			out = new BufferedWriter(new FileWriter(new File(resultFile)));
			while (ratings.peek() != null) {
				out.write(ratings.poll().toString() + "\n");
			}
		} finally {
			if (out != null)
				out.close();
		}

	}
}
