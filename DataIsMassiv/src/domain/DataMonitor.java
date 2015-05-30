package domain;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class DataMonitor {
	TreeMap<Integer, ProbeData> interaction = new TreeMap<>();

	public static class ProbeData {
		int day = 0;
		AVGPair is;
		AVGPair should;
	}

	public void reportInteraction(int dateId, double prediction, double rating) {
		ProbeData p = getInteraction(dateId);
		p.is.add(prediction * prediction);
		p.should.add(rating * rating);
	}

	private ProbeData getInteraction(int dateId) {
		ProbeData p = interaction.get(dateId);
		if (p == null) {
			p = new ProbeData();
			p.day = dateId;
			p.is = new AVGPair();
			p.should = new AVGPair();
			interaction.put(dateId, p);
		}
		return p;
	}

	public void printReportTo(BufferedWriter writer) throws IOException {
		for (Map.Entry<Integer, ProbeData> e : interaction.entrySet()) {
			ProbeData value = e.getValue();
			double avgIs = Math.sqrt(value.is.getAVG());
			double avgShould = Math.sqrt(value.should.getAVG());
			writer.append(value.day + "\t" + avgIs + "\t"
					+ avgShould + "\n");
		}
	}

}
