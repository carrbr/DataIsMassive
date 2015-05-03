package helper;

import static org.junit.Assert.*;

import org.junit.Test;

public class SparseVectorTest {

	@Test
	public void testClear() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		sv.clear();
		assertEquals(0, sv.size);
	}

	@Test
	public void testContainsTrue() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		assertTrue(sv.contains(1));
	}
	
	@Test
	public void testContainsFalse() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		assertFalse(sv.contains(17));
	}

	@Test
	public void testIsEmptyFalse() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		assertFalse(sv.isEmpty());
	}

	@Test
	public void testIsEmptyTrue() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		assertTrue(sv.isEmpty());
	}
	
	@Test
	public void testSizeContiguous() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		assertEquals(3, sv.size());
	}

	@Test
	public void testSizeNonContiguous() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		sv.add(19, 19);
		assertEquals(20, sv.size());
	}

	@Test
	public void testGetElemWithValue() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		sv.add(19, 19);
		assertEquals(2, sv.get(2).intValue());
	}
	
	@Test
	public void testGetSparseElem() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		sv.add(19, 19);
		assertEquals(null, sv.get(17));
	}

	@Test
	public void testIndexOfNormalElem() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		sv.add(19, 19);
		assertEquals(19, sv.indexOf(19));
	}
	
	@Test
	public void testIndexOfSparseElem() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		sv.add(19, 19);
		assertEquals(3, sv.indexOf(null));
	}
	
	@Test
	public void testIndexOfNotExists() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		sv.add(19, 19);
		assertEquals(-1, sv.indexOf(17));
	}
	
	@Test
	public void testLastIndexOfNormalElem() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 4);
		sv.add(2, 2);
		sv.add(4, 4);
		sv.add(19, 19);
		assertEquals(4, sv.lastIndexOf(4));
	}
	
	@Test
	public void testLastIndexOfSparseElem() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 4);
		sv.add(2, 2);
		sv.add(4, 4);
		sv.add(19, 19);
		assertEquals(18, sv.lastIndexOf(null));
	}
	
	@Test
	public void testLastIndexOfNotExists() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 1);
		sv.add(2, 2);
		sv.add(19, 19);
		assertEquals(-1, sv.lastIndexOf(17));
	}


	@Test
	public void testRemoveInt() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 4);
		sv.add(2, 2);
		sv.add(4, 4);
		sv.add(19, 19);
		assertEquals(4, sv.remove(1).intValue());
		assertEquals(null, sv.get(1));
	}

	@Test
	public void testSet() {
		SparseVector<Integer> sv = new SparseVector<Integer>();
		sv.add(0, 0);
		sv.add(1, 4);
		sv.add(2, 2);
		sv.add(4, 4);
		sv.add(19, 19);
		sv.set(4, 3);
		assertEquals(3, sv.get(4).intValue());
	}

}
