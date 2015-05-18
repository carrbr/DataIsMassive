package helper;

import java.util.Iterator;

import domain.Rating;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.ISparseVector;
import no.uib.cipr.matrix.sparse.SparseVector;

public class SparseRatingVector implements ISparseVector {
	
	private static final long serialVersionUID = -3254246141380671408L;
	
	SparseVector ratingValues;
	Rating[] ratings;

	public SparseRatingVector(int size, int[] index, Rating[] ratings) {
		
	}
	
	@Override
	public Vector add(Vector arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(int arg0, double arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Vector add(double arg0, Vector arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double dot(Vector arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double get(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double norm(Norm arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Vector scale(double arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector set(Vector arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(int arg0, double arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Vector set(double arg0, Vector arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Vector zero() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<VectorEntry> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getIndex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getUsed() {
		// TODO Auto-generated method stub
		return 0;
	}

}
