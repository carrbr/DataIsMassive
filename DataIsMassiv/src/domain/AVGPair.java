package domain;

import java.io.Serializable;

public class AVGPair implements Serializable {
	private static final long serialVersionUID = -2861423457296709820L;
	int count = 0;
	double totalScore = 0;

	public void add(double score) {
		count++;
		totalScore += score;
	}

	public double getAVG() {
		return totalScore / count;
	}
}
