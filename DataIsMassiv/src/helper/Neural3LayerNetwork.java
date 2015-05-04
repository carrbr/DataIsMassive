package helper;

import java.io.Serializable;
import java.util.Random;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.RealVectorChangingVisitor;

public class Neural3LayerNetwork implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final double eta = .5;
	private static double a = .3;
	private RealMatrix hh;
	private RealMatrix h;
	private RealMatrix o;

	public Neural3LayerNetwork(int i, int hh, int h, int o) {
		this.hh = MatrixUtils.createRealMatrix(hh, i + 1);
		this.h = MatrixUtils.createRealMatrix(h, hh + 1);
		this.o = MatrixUtils.createRealMatrix(o, h + 1);
		scrambeAll();
	}

	public void scrambeAll() {
		scramble(hh);
		scramble(h);
		scramble(o);
	}

	public void scramble(RealMatrix x) {
		// replace with default entry iterator
		Random r = new Random();
		for (int i = 0; i < x.getRowDimension(); i++) {
			for (int j = 0; j < x.getColumnDimension(); j++) {
				x.setEntry(i, j, (r.nextDouble() - .5));
			}
		}
	}

	/**
	 * heart of the network, this calculates the response the error and the
	 * gradient descent for the network
	 * 
	 * @param input
	 *            stimulation of the network
	 * @param response
	 *            hoped for response
	 * @return delta for input to get better result
	 */
	public RealVector learn(RealVector input, RealVector response) {
		// Responses
		input = input.append(1); // Adding Neuron Baias to input
		RealVector outhidhid = hh.operate(input);
		sigmoid(outhidhid);

		outhidhid = outhidhid.append(1);
		RealVector outhid = h.operate(outhidhid);
		sigmoid(outhid);

		outhid = outhid.append(1);
		RealVector out = o.operate(outhid);
		sigmoid(out);

		// Error
		RealVector error = response.subtract(out);

		// Correction
		RealVector outdelta = sigmoDiff(out).ebeMultiply(error);
		RealVector hiddelta = chainDiff(outhid, outdelta, o);
		RealVector hidhiddelta = chainDiff(outhidhid, dropLast(hiddelta), h);
		RealVector inputdelta = chainDiff(input, dropLast(hidhiddelta), hh);

		// Current + Correction = next
		o = o.add(outdelta.outerProduct(outhid.mapMultiply(eta)));
		h = h.add(dropLast(hiddelta.outerProduct(outhidhid.mapMultiply(eta))));
		hh = hh.add(dropLast(hidhiddelta.outerProduct(input.mapMultiply(eta))));
		return dropLast(inputdelta);
	}

	private RealMatrix dropLast(RealMatrix m) {
		return m.getSubMatrix(0, m.getRowDimension() - 2, 0,
				m.getColumnDimension() - 1);
	}

	private static RealVector dropLast(RealVector delta) {
		return delta.getSubVector(0, delta.getDimension() - 1);
	}

	/**
	 * derivation of sigmoid to find the way down (gradient decent)
	 * 
	 * @param out
	 * @return gradient
	 */
	private static RealVector sigmoDiff(RealVector out) {
		return out.ebeMultiply(out.mapSubtract(1).mapMultiply(-1));
	}

	/**
	 * derivation of layers behind layers.
	 * 
	 * @param outLayer
	 * @param chainLowerLayerDelta
	 * @param LayerMatrix
	 * @return gradient
	 */
	private static RealVector chainDiff(RealVector outLayer,
			RealVector chainLowerLayerDelta, RealMatrix LayerMatrix) {
		RealVector chain = LayerMatrix.transpose()
				.operate(chainLowerLayerDelta);
		return sigmoDiff(outLayer).ebeMultiply(chain);
	}

	/**
	 * non linear function for higher order approximation
	 * 
	 * @param vector
	 */
	private static void sigmoid(RealVector vector) {
		vector.walkInDefaultOrder(new RealVectorChangingVisitor() {

			@Override
			public double visit(int index, double value) {
				return 1 / (1 + Math.exp(-a * value));
			}

			@Override
			public void start(int dimension, int start, int end) {
			}

			@Override
			public double end() {
				return 0;
			}
		});
	}
}
