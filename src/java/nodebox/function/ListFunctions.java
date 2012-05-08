package nodebox.function;


import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.*;

/**
 * Operations on lists.
 * <p/>
 * When we say lists, we really mean Iterables, the most generic type of lists.
 * We do this because some functions return infinite Iterables.
 * <p/>
 * Functions return the most specific type: for example, sort needs to build up a list from the input Iterable,
 * so it will return this list, avoiding the consumer to create another copy for processing.
 * <p/>
 * Functions here are not concerned what is inside the  items of the list.
 * They operate on the lists themselves, not on the contents of the list elements.
 */
public class ListFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("list", ListFunctions.class,
                "count",
                "first", "second", "rest", "last",
                "combine", "slice", "shift",
                "distinct", "repeat", "reverse", "sort", "shuffle", "pick", "filter", "cull", "takeEvery");
    }

    /**
     * Count the number of items in the list.
     * <p/>
     * If the list is infinite, this function will keep on running forever.
     *
     * @param iterable The list items.
     * @return The total amount of items in the list.
     */
    public static long count(Iterable<?> iterable) {
        if (iterable == null) return 0;
        return Iterables.size(iterable);
    }

    /**
     * Take the first item of the list.
     *
     * @param iterable The list items.
     * @return A new list with only the first item.
     */
    public static List<?> first(Iterable<?> iterable) {
        if (iterable == null) return ImmutableList.of();
        Iterator iterator = iterable.iterator();
        if (iterator.hasNext()) {
            return ImmutableList.of(iterator.next());
        }
        return ImmutableList.of();
    }

    /**
     * Take the second item of the list.
     *
     * @param iterable The list items.
     * @return A new list with only the second item.
     */
    public static List<?> second(Iterable<?> iterable) {
        if (iterable == null) return ImmutableList.of();
        Iterator iterator = iterable.iterator();
        if (iterator.hasNext()) {
            iterator.next();
            if (iterator.hasNext()) {
                return ImmutableList.of(iterator.next());
            }
        }
        return ImmutableList.of();
    }

    /**
     * Take all but the first item of the list.
     *
     * @param iterable The list items.
     * @return A new list with the first item skipped.
     */
    public static Iterable<?> rest(Iterable<?> iterable) {
        if (iterable == null) return ImmutableList.of();
        return Iterables.skip(iterable, 1);
    }


    /**
     * Take the last item of the list.
     *
     * @param iterable The list items.
     * @return A new list with only the last item.
     */
    public static List<?> last(Iterable<?> iterable) {
        if (iterable == null) return ImmutableList.of();
        try {
            return ImmutableList.of(Iterables.getLast(iterable));
        } catch (NoSuchElementException e) {
            return ImmutableList.of();
        }
    }

    public static Iterable<?> combine(Iterable list1, Iterable list2, Iterable list3) {
        Iterable<Iterable<?>> nonNullLists = Iterables.filter(Lists.<Iterable<?>>newArrayList(list1, list2, list3), Predicates.notNull());
        return Iterables.concat(nonNullLists);
    }

    /**
     * Combine multiple lists into one.
     *
     * @param iterables The lists to combine.
     * @return A new list with all input lists combined.
     */
    public static Iterable<?> combine(Iterable<?>... iterables) {
        return Iterables.concat(iterables);
    }

    /**
     * Take a portion of the original list.
     *
     * @param iterable   The list items.
     * @param startIndex The starting index, zero-based.
     * @param size       The amount of items.
     * @return A new list containing a slice of the original.
     */
    public static Iterable<?> slice(Iterable<?> iterable, long startIndex, long size) {
        if (iterable == null) return ImmutableList.of();
        Iterable<?> skipped = Iterables.skip(iterable, (int) startIndex);
        return Iterables.limit(skipped, (int) size);
    }

    /**
     * Take the beginning elements from the beginning of the list and append them to the end of the list.
     *
     * @param iterable The list items.
     * @param amount   The amount of items to shift.
     * @return A new list with the items shifted.
     */
    public static Iterable<?> shift(Iterable<?> iterable, long amount) {
        if (iterable == null) return ImmutableList.of();
        int listSize = Iterables.size(iterable);
        if (listSize == 0) return ImmutableList.of();
        int a = (int) amount % listSize;
        if (a == 0) return iterable;
        Iterable<?> tail = Iterables.skip(iterable, a);
        Iterable<?> head = Iterables.limit(iterable, a);
        return Iterables.concat(tail, head);
    }

    /**
     * Repeat the given sequence amount times.
     *
     * @param iterable The list items.
     * @param amount   The amount of repetitions.
     * @return A new list with the items repeated.
     */
    public static Iterable<?> repeat(Iterable<?> iterable, long amount) {
        if (iterable == null) return ImmutableList.of();
        if (amount < 1) return ImmutableList.of();
        Iterable<?>[] iterables = new Iterable<?>[(int) amount];
        for (int i = 0; i < amount; i++) {
            iterables[i] = iterable;
        }
        return Iterables.concat(iterables);
    }

    /**
     * Reverse the items in the list.
     *
     * @param iterable The list items.
     * @return A new list with the items reversed.
     */
    public static List<?> reverse(Iterable<?> iterable) {
        if (iterable == null) return ImmutableList.of();
        return Lists.reverse(ImmutableList.copyOf(iterable));
    }

    /**
     * Sort items in the list.
     * If a key is provided, sort according to this key,
     * otherwise perform sorting by natural sort order.
     * <p/>
     *
     * @param iterable The list items.
     * @param key      The key by which to order.
     * @return A new, sorted list.
     */
    @SuppressWarnings("unchecked")
    public static List<?> sort(Iterable<?> iterable, final String key) {
        if (iterable == null) return ImmutableList.of();
        try {
            if (key == null || key.length() == 0)
                return ImmutableList.copyOf(Ordering.natural().sortedCopy((Iterable<? extends Comparable>) iterable));
            Ordering<Object> keyOrdering = new Ordering<Object>() {
                public int compare(Object o1, Object o2) {
                    Comparable c1 = (Comparable) DataFunctions.lookup(o1, key);
                    Comparable c2 = (Comparable) DataFunctions.lookup(o2, key);
                    return Ordering.natural().compare(c1, c2);
                }
            };
            return ImmutableList.copyOf(keyOrdering.sortedCopy(iterable));
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Invalid key for this type of object: " + key);
        } catch (ClassCastException e) {
            // This error occurs when an element is not a Comparable or when
            // there are elements of different types in the list.
            throw new IllegalArgumentException("To sort a list, all elements in the list need to be comparable and of the same type.");
        }
    }

    /**
     * Shuffle the items in the list.
     * <p/>
     * Shuffling is stable: using the same seed will always return items in the same sort order.
     *
     * @param iterable The items to shuffle.
     * @param seed     The random seed.
     * @return A new iterable with items in random order.
     */
    public static List<?> shuffle(Iterable<?> iterable, long seed) {
        if (iterable == null) return ImmutableList.of();
        List<?> l = Lists.newArrayList(iterable);
        Collections.shuffle(l, new Random(seed));
        return ImmutableList.copyOf(l);
    }

    /**
     * Pick a number of items from the list.
     * <p/>
     * Shuffling is stable: using the same seed will always return items in the same sort order.
     *
     * @param iterable The items to pick from.
     * @param amount   The amount of items.
     * @param seed     The random seed.
     * @return A new iterable with specified amount of items in random order.
     */
    public static List<?> pick(Iterable<?> iterable, long amount, long seed) {
        if (iterable == null || amount <= 0) return ImmutableList.of();
        List<?> l = Lists.newArrayList(iterable);
        Collections.shuffle(l, new Random(seed));
        if (amount >= l.size()) return ImmutableList.copyOf(l);
        return ImmutableList.copyOf(l.subList(0, (int) amount));
    }

    /**
     * Cycle indefinitely over the elements of the list. This creates an infinite list.
     *
     * @param iterable The list items.
     * @return A new infinite iterable.
     */
    public static Iterable<?> cycle(Iterable<?> iterable) {
        if (iterable == null) return ImmutableList.of();
        return Iterables.cycle(iterable);
    }

    /**
     * Filter a list of elements using a true/false pattern.
     *
     * @param iterable The list to filter.
     * @param booleans The pattern that determines which elements are retained and which are not.
     * @return The filtered list.
     */
    public static List<?> filter(Iterable<?> iterable, Iterable<Boolean> booleans) {
        if (iterable == null) return ImmutableList.of();
        if (booleans == null) return ImmutableList.copyOf(iterable);
        if (Iterables.size(booleans) == 1) {
            if (Iterables.getFirst(booleans, false))
                return ImmutableList.copyOf(iterable);
            else
                return ImmutableList.of();
        }
        List<?> l = Lists.newArrayList(iterable);
        List<Boolean> b = Lists.newArrayList(booleans);
        List<Object> newList = new ArrayList<Object>();
        int min = Math.min(l.size(), b.size());
        for (int i = 0; i < min; i++) {
            boolean keep = b.get(i);
            if (keep)
                newList.add(l.get(i));
        }
        if (b.size() < l.size())
            newList.addAll(l.subList(b.size(), l.size()));
        return ImmutableList.copyOf(newList);
    }

    /**
     * Cull a list of elements using a true/false pattern.
     *
     * @param iterable The list to filter.
     * @param booleans The pattern that determines which elements are retained and which are not.
     * @return The culled list.
     */
    public static List<?> cull(Iterable<?> iterable, Iterable<Boolean> booleans) {
        if (iterable == null) return ImmutableList.of();
        if (booleans == null) return ImmutableList.copyOf(iterable);
        if (Iterables.size(booleans) == 1) {
            if (Iterables.getFirst(booleans, false))
                return ImmutableList.copyOf(iterable);
            else
                return ImmutableList.of();
        }
        Iterator<?> it = iterable.iterator();
        ImmutableList.Builder<Object> builder = ImmutableList.builder();
        Iterator<?> booleanIterator = ((Iterable<?>) cycle(booleans)).iterator();
        if (!booleanIterator.hasNext()) return ImmutableList.copyOf(iterable);
        while (it.hasNext()) {
            Object object = it.next();
            boolean keep = (Boolean) booleanIterator.next();
            if (keep) builder.add(object);
        }
        return builder.build();
    }

    /**
     * Filter a list so it only contains unique elements (no duplicates).
     *
     * @param iterable The list to filter.
     * @return The filtered list.
     */
    public static List<?> distinct(Iterable<?> iterable) {
        if (iterable == null) return ImmutableList.of();
        List<Object> newList = new ArrayList<Object>();
        Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (newList.contains(object)) continue;
            newList.add(object);
        }
        return ImmutableList.copyOf(newList);
    }

    public static Iterable<?> takeEvery(Iterable<?> iterable, long n) {
        if (iterable == null) return ImmutableList.of();
        ImmutableList.Builder<Object> b = ImmutableList.builder();
        Iterator<?> iterator = iterable.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (i % n == 0) {
                b.add(o);
            }
            i++;
        }
        return b.build();
    }

}
