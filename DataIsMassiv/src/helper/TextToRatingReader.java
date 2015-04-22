package helper;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import domain.Rating;

public class TextToRatingReader implements Closeable {

	private BufferedReader reader;

	public TextToRatingReader(String file) throws FileNotFoundException {
		reader = new BufferedReader(new FileReader(new File(file)));
	}

	public Rating readNext() throws IOException {
		String s = reader.readLine();
		if (s== null || !s.contains(","))
			return null;
		int subi[] = generateInt(s.split(","));
		if (subi.length == 4) {
			return new Rating(subi[0], subi[1], subi[2], (short) subi[3]);
		}
		if (subi.length == 3) {
			return new Rating(subi[0], subi[1], subi[2]);
		}
		return null;
	}

	private int[] generateInt(String[] sub) {
		int i[] = new int[sub.length];
		for (int j = 0; j < sub.length; j++) {
			i[j] = Integer.parseInt(sub[j]);
		}
		return i;
	}

	@Override
	public void close() throws IOException {
		reader.close();

	}
}
