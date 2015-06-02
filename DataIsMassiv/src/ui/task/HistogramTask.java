package ui.task;

import helper.TextToRatingReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import domain.Rating;

public class HistogramTask extends TaskCommand {
	private String toAnalize;
	private String outFile;
	private int dayBucketSize;

	public HistogramTask(String[] args) {
		super(args);

		if (!needsHelp && args.length == 3) {
			this.toAnalize = args[0];
			this.outFile = args[1];
			this.dayBucketSize = Integer.parseInt(args[2]);
		}
	}

	@Override
	public void exec() throws Exception {
		if (writeHelpIfNeeded())
			return;

		TextToRatingReader in = null;
		BufferedWriter out = null;

		try {
			in = new TextToRatingReader(toAnalize);
			out = new BufferedWriter(new FileWriter(new File(outFile)));
			TreeMap<Integer, Integer> counts = countData(in, dayBucketSize);
			writeOut(counts, out);

		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}

	private void writeOut(TreeMap<Integer, Integer> counts, BufferedWriter out)
			throws IOException {
		for (Map.Entry<Integer, Integer> pair : counts.entrySet()) {
			out.write(pair.getKey() + "\t" + pair.getValue()+"\n");
		}

	}

	private TreeMap<Integer, Integer> countData(TextToRatingReader in,
			int dayBucketSize2) throws IOException {
		TreeMap<Integer, Integer> count = new TreeMap<>();
		Rating r = null;
		while ((r = in.readNext()) != null) {
			Integer p = count.get(r.getDateId() / dayBucketSize2);
			if (p == null) {
				p = 1;
				count.put(r.getDateId() / dayBucketSize2, p);
			}
			count.put(r.getDateId() / dayBucketSize2, p + 1);
		}

		return count;
	}

}
