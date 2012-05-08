package nodebox.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An infinite list with one element over and over.
 */
public final class InfiniteList<E> implements List<E> {


    public static <E> InfiniteList<E> of(E object) {
        return new InfiniteList<E>(object);
    }

    private final E theObject;


    public InfiniteList(E theObject) {
        checkNotNull(theObject);
        this.theObject = theObject;
    }

    public int size() {
        return Integer.MAX_VALUE;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean contains(Object o) {
        return theObject.equals(o);
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            public boolean hasNext() {
                return true;
            }

            public E next() {
                return theObject;
            }

            public void remove() {
                throw new UnsupportedOperationException("Remove is not supported.");
            }
        };
    }


    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    public <E> E[] toArray(E[] a) {
        throw new UnsupportedOperationException();

    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public boolean containsAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection collection) {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public boolean addAll(int i, Collection collection) {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public boolean removeAll(Collection<?> objects) {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public boolean retainAll(Collection<?> objects) {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public E get(int i) {
        return theObject;
    }

    public E set(int i, Object o) {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public void add(int i, Object o) {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public E remove(int i) {
        throw new UnsupportedOperationException("Infinite list is read-only.");
    }

    public int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException();
    }

    public ListIterator<E> listIterator(int i) {
        throw new UnsupportedOperationException();
    }

    public List<E> subList(int i, int i1) {
        throw new UnsupportedOperationException();
    }
}
