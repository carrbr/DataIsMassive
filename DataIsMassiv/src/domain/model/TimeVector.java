package domain.model;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

public class TimeVector {
	private static final double[][] para = { { 4984, 0, 1.1, 2.5 },
			{ 1460.97, .5, 1.8, 3.3 }, { 365.243, .2, 1.35, 2.6 },
			{ 29.6, 0, .9, 1.6 }, { 7, .2, .7, 2.1 }, { 3, .1, 1.5, 2.9 } };
	public static final int timeVecDim = para.length * (para[0].length - 1);

	public static RealVector createVectorOn(int time) {
		// First in 4 is frequeny, other 3 are phase translation

		double[] data = new double[para.length * 3];
		for (int i = 0; i < para.length; i++) {
			double localPhase = time / para[i][0] * 2 * Math.PI;
			data[i * 3 + 0] = Math.sin(localPhase + para[i][1]) / 2 + .5;
			data[i * 3 + 1] = Math.sin(localPhase + para[i][2]) / 2 + .5;
			data[i * 3 + 2] = Math.sin(localPhase + para[i][3]) / 2 + .5;
		}
		return MatrixUtils.createRealVector(data);
	}

}
