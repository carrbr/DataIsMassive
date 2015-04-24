package ui.task;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import domain.Rating;

public class PublishResultTask extends TaskCommand {
	private static String listNum = "7120309055_7120309030_5113709785_5113709081.txt";
	private String toPublish;
	private String directory;

	public PublishResultTask(String toPublish, String directory) {
		this.toPublish = toPublish;
		this.directory = directory;

	}

	public PublishResultTask(String[] args) {
		if (needsHelp) {
			writeHelp();
			return;
		}
		needsHelp = needsHelp(args);

		if (!needsHelp && args.length == 2) {
			this.toPublish = args[0];
			this.directory = args[1];
		}
	}

	@Override
	public void exec() throws Exception {
		TextToRatingReader in = null;
		BufferedWriter out = null;

		try {
			in = new TextToRatingReader(toPublish);
			out = new BufferedWriter(new FileWriter(new File(directory
					+ listNum)));
			Rating rate = null;
			while ((rate = in.readNext()) != null) {
				out.write(rate.getNiceFormatRating() + "\n");
			}

		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}
}
