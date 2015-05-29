package helper;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import domain.Rating;
import domain.SimilarityRating;

public class TextToRatingReader implements Closeable {

	private BufferedReader reader;

	public TextToRatingReader(String file) throws FileNotFoundException {
		reader = new BufferedReader(new FileReader(new File(file)));
	}

	public Rating readNext() throws IOException {
		String s = reader.readLine();
		if (s == null || !s.contains(","))
			return null;
		String subi[] = s.split(",");
		if (subi.length == 5) {
			return new SimilarityRating(Double.parseDouble(subi[0]),
					Integer.parseInt(subi[1]), Integer.parseInt(subi[2]),
					Integer.parseInt(subi[3]), Float.parseFloat(subi[4]));
		}
		if (subi.length == 4) {
			return new Rating(Integer.parseInt(subi[0]),
					Integer.parseInt(subi[1]), Integer.parseInt(subi[2]),
					Float.parseFloat(subi[3]));
		}
		if (subi.length == 3) {
			return new Rating(Integer.parseInt(subi[0]),
					Integer.parseInt(subi[1]), Integer.parseInt(subi[2]));
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		reader.close();

	}
}
