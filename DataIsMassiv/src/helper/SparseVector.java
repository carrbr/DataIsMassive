package helper;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

public class SparseVector<E> implements List<E> {
	Map<Integer, E> data;	// internal data representation
	int size;	// current size of the vector
	
	public SparseVector() {
		this.data = new TreeMap<Integer, E>();
		this.size = 0;
	}

	@Override
	public void clear() {
		this.size = 0;
		this.data.clear();
	}

	@Override
	public boolean contains(Object arg0) {
		return this.data.containsValue(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		boolean result = true;
		
		for(int i = 0; i < arg0.size(); i++) {
			if (!this.data.containsValue(arg0)) {
				result = false;
				break;
			}
		}
		
		return result;
	}

	@Override
	public boolean isEmpty() {
		boolean result = false;
		
		if (this.size == 0) {
			result = true;
		}
		
		return result;
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		return this.size;
	}

	/**
	 * Be careful with this method as returned array will no longer have a sparse representation
	 * 
	 * THIS MEANS IT MAY BE ORDERS OF MAGNITUDE LARGER THEN THE SPARSEVECTOR
	 * 
	 * @return
	 */
	@Override
	public Object[] toArray() {
		Object[] array = new Object[this.size];
		
		// fill up array
		for (int i = 0; i < this.size; i++) {
			if (this.data.containsKey(i)) { // this value exists in data rep
				array[i] = this.data.get(i);
			} else { // we do not have a value for this index, replace with null
				array[i] = null;
			}
		}
		return array;
	}
	
	@Override
	public void add(int arg0, E arg1) {
		this.data.put(arg0, arg1);
		
		// make sure our array size accounting is up to data
		if (arg0 >= this.size) {
			this.size = arg0 + 1;
		}
	}

	@Override
	public E get(int arg0) {
		if (arg0 >= this.size) { // out of bounds
			throw new IndexOutOfBoundsException();
		}
		
		if (this.data.containsKey(arg0)) {
			return this.data.get(arg0); // we have an actual value for this index
		}
		return null; // index is in vector, but due to sparseness we don't have a value
	}

	@Override
	public int indexOf(Object arg0) {
		if (this.data.containsKey(arg0)) {
			for (int i = 0; i < this.size; i++) {
				if (this.data.get(i).equals(arg0)) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object arg0) {
		if (this.data.containsKey(arg0)) {
			for (int i = this.size - 1; i < this.size; i--) {
				if (this.data.get(i).equals(arg0)) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Note that this method can't actually completely remove an entry from the vector
	 * Rather, it will only zero that element out in the sparse vector
	 * @param arg0
	 * @return
	 */
	@Override
	public E remove(int arg0) {
		if (arg0 == this.size - 1) {
			size--;
		}
		E elem = null;
		if (this.data.containsKey(arg0)) {
			elem = this.data.remove(arg0);
		}
		return elem;
	}

	@Override
	public E set(int arg0, E arg1) {
		if (arg0 >= this.size) {
			throw new IndexOutOfBoundsException();
		}
		if (this.data.containsKey(arg0)) {
			this.data.remove(arg0);
		}
		this.data.put(arg0, arg1);
		return null;
	}

	@Override
	public boolean addAll(Collection<? extends E> arg0) {
		throw new UnsupportedOperationException("Unimplemented Optional Method");
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException("Unimplemented Optional Method");
	}
	
	@Override
	public boolean remove(Object arg0) {
		throw new UnsupportedOperationException("Unimplemented Optional Method");
	}
	
	@Override
	public boolean removeAll(Collection<?> arg0) {
		throw new UnsupportedOperationException("Unimplemented Optional Method");

	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		throw new UnsupportedOperationException("Unimplemented Optional Method");
	}
	
	@Override
	public boolean addAll(int arg0, Collection<? extends E> arg1) {
		throw new UnsupportedOperationException("Unimplemented Optional Method");
	}
	
	@Override
	public <T> T[] toArray(T[] arg0) {
		/*
		 * This isn't actually an optional method but I don't think we'll need
		 * it for anything so I'm going to be lazy... sorry :P
		 */
		throw new UnsupportedOperationException("Unimplemented Optional Method");
	}
	
	/*
	 * I'm actually not implementing any of the iterator methods right now because I'm a bad person.
	 */
	@Override
	public ListIterator<E> listIterator() {
		throw new UnsupportedOperationException("Unimplemented Optional Method");
	}

	@Override
	public ListIterator<E> listIterator(int arg0) {
		throw new UnsupportedOperationException("Unimplemented Optional Method");
	}
	
	/*
	 * same story on this function.  I'm a terrible human being.
	 */
	@Override
	public List<E> subList(int arg0, int arg1) {
		throw new UnsupportedOperationException("Unimplemented Optional Method");
	}
}
