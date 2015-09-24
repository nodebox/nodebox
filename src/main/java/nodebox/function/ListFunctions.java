package nodebox.function;


import com.google.common.base.Predicates;
import com.google.common.collect.*;
import nodebox.util.MathUtils;
import nodebox.util.ReflectionUtils;

import java.util.*;

/**
 * Operations on lists.
 * <p>
 * When we say lists, we really mean Iterables, the most generic type of lists.
 * We do this because some functions return infinite Iterables.
 * <p>
 * Functions return the most specific type: for example, sort needs to build up a list from the input Iterable,
 * so it will return this list, avoiding the consumer to create another copy for processing.
 * <p>
 * Functions here are not concerned what is inside the  items of the list.
 * They operate on the lists themselves, not on the contents of the list elements.
 */
public class ListFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("list", ListFunctions.class,
                "count",
                "first", "second", "rest", "last",
                "combine", "slice", "shift", "doSwitch",
                "distinct", "repeat", "reverse", "sort", "shuffle", "pick", "cull", "takeEvery",
                "keys", "zipMap");
    }

    /**
     * Count the number of items in the list.
     * <p>
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
     * @return The first item of the list.
     */
    public static Object first(Iterable<?> iterable) {
        if (iterable == null) return null;
        Iterator iterator = iterable.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    /**
     * Take the second item of the list.
     *
     * @param iterable The list items.
     * @return The second item of the list.
     */
    public static Object second(Iterable<?> iterable) {
        if (iterable == null) return null;
        Iterator iterator = iterable.iterator();
        if (iterator.hasNext()) {
            iterator.next();
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        return null;
    }

    /**
     * Take all but the first item of the list.
     *
     * @param iterable The list items.
     * @return A new list with the first item skipped.
     */
    public static List<?> rest(Iterable<?> iterable) {
        if (iterable == null) return ImmutableList.of();
        return ImmutableList.copyOf(Iterables.skip(iterable, 1));
    }

    /**
     * Take the last item of the list.
     *
     * @param iterable The list items.
     * @return The last item of the list.
     */
    public static Object last(Iterable<?> iterable) {
        if (iterable == null) return null;
        try {
            return Iterables.getLast(iterable);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Combine multiple lists into one.
     *
     * @param list1 The first list to combine.
     * @param list2 The second list to combine.
     * @param list3 The third list to combine.
     * @param list4 The fourth list to combine.
     * @param list5 The fifth list to combine.
     * @param list6 The sixth list to combine.
     * @param list7 The seventh list to combine.
     * @return A new list with all input lists combined.
     */
    public static List<?> combine(Iterable list1, Iterable list2, Iterable list3,
                                  Iterable list4, Iterable list5, Iterable list6,
                                  Iterable list7) {
        Iterable<Iterable<?>> nonNullLists = Iterables.filter(
                Lists.<Iterable<?>>newArrayList(list1, list2, list3, list4, list5, list6, list7),
                Predicates.notNull());
        return ImmutableList.copyOf(Iterables.concat(nonNullLists));
    }

    /**
     * Take a portion of the original list.
     *
     * @param iterable   The list items.
     * @param startIndex The starting index, zero-based.
     * @param size       The amount of items.
     * @param invert     Omit the given list items instead of retaining them.
     * @return A new list containing a slice of the original.
     */
    public static List<?> slice(Iterable<?> iterable, long startIndex, long size, boolean invert) {
        if (iterable == null) return ImmutableList.of();
        if (!invert) {
            Iterable<?> skipped = Iterables.skip(iterable, (int) startIndex);
            return ImmutableList.copyOf(Iterables.limit(skipped, (int) size));
        } else {
            Iterable<?> firstList = Iterables.limit(iterable, (int) startIndex);
            Iterable<?> secondList = Iterables.skip(iterable, (int) (startIndex + size));
            return ImmutableList.copyOf(Iterables.concat(firstList, secondList));
        }
    }

    /**
     * Take the beginning elements from the beginning of the list and append them to the end of the list.
     *
     * @param iterable The list items.
     * @param amount   The amount of items to shift.
     * @return A new list with the items shifted.
     */
    public static List<?> shift(Iterable<?> iterable, long amount) {
        if (iterable == null) return ImmutableList.of();
        int listSize = Iterables.size(iterable);
        if (listSize == 0) return ImmutableList.of();
        int a = (int) amount % listSize;
        if (a < 0) {
            a += listSize;
        }
        if (a == 0) return ImmutableList.copyOf(iterable);
        Iterable<?> tail = Iterables.skip(iterable, a);
        Iterable<?> head = Iterables.limit(iterable, a);
        return ImmutableList.copyOf(Iterables.concat(tail, head));
    }

    /**
     * Switch between multiple inputs.
     *
     * @param list1 The first input list.
     * @param list2 The second input list.
     * @param list3 The third input list.
     * @param list4 The fourth input list.
     * @param list5 The fifth input list.
     * @param list6 The sixth input list.
     * @param index The index of the input list to return.
     * @return A list with the specified index.
     */
    public static List<?> doSwitch(Iterable list1, Iterable list2, Iterable list3,
                                   Iterable list4, Iterable list5, Iterable list6,
                                   long index) {
        Iterable<?> returnList;
        switch ((int) index % 6) {
            case 0:
                returnList = list1;
                break;
            case 1:
                returnList = list2;
                break;
            case 2:
                returnList = list3;
                break;
            case 3:
                returnList = list4;
                break;
            case 4:
                returnList = list5;
                break;
            case 5:
                returnList = list6;
                break;
            default:
                throw new AssertionError();
        }
        if (returnList == null) return ImmutableList.of();
        return ImmutableList.copyOf(returnList);
    }

    /**
     * Repeat the given sequence amount times.
     *
     * @param iterable The list items.
     * @param amount   The amount of repetitions.
     * @param perItem  Repeats the items one after another, e.g. aabbcc instead of ababab (the default).
     * @return A new list with the items repeated.
     */
    public static List<?> repeat(Iterable<?> iterable, long amount, boolean perItem) {
        if (iterable == null) return ImmutableList.of();
        if (amount < 1) return ImmutableList.of();
        if (amount == 1) return ImmutableList.copyOf(iterable);
        if (perItem) {
            Iterator iterator = iterable.iterator();
            ImmutableList.Builder<Object> builder = ImmutableList.builder();
            while (iterator.hasNext()) {
                Object o = iterator.next();
                for (int i = 0; i < amount; i++)
                    builder.add(o);
            }
            return builder.build();
        } else {
            Iterable<?>[] iterables = new Iterable<?>[(int) amount];
            for (int i = 0; i < amount; i++) {
                iterables[i] = iterable;
            }
            return ImmutableList.copyOf(Iterables.concat(iterables));
        }
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
     * <p>
     *
     * @param iterable The list items.
     * @param key      The key by which to order.
     * @return A new, sorted list.
     */
    @SuppressWarnings("unchecked")
    public static List<?> sort(Iterable<?> iterable, final String key) {
        if (iterable == null) return ImmutableList.of();
        try {
            if (key == null || key.length() == 0) {
                Object first = Iterables.getFirst(iterable, null);
                if (first instanceof Map) {
                    Object firstKey = Iterables.getFirst(((Map) first).keySet(), null);
                    if (firstKey != null && firstKey instanceof String)
                        return sort(iterable, (String) firstKey);
                }
                return ImmutableList.copyOf(Ordering.natural().sortedCopy((Iterable<? extends Comparable>) iterable));
            }
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
     * <p>
     * Shuffling is stable: using the same seed will always return items in the same sort order.
     *
     * @param iterable The items to shuffle.
     * @param seed     The random seed.
     * @return A new iterable with items in random order.
     */
    public static List<?> shuffle(Iterable<?> iterable, long seed) {
        if (iterable == null) return ImmutableList.of();
        List<?> l = Lists.newArrayList(iterable);
        Collections.shuffle(l, MathUtils.randomFromSeed(seed));
        return ImmutableList.copyOf(l);
    }

    /**
     * Pick a number of items from the list.
     * <p>
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
        Collections.shuffle(l, MathUtils.randomFromSeed(seed));
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
     * Cull a list of elements using a true/false pattern.
     *
     * @param iterable The list to filter.
     * @param booleans The pattern that determines which elements are retained and which are not.
     * @return The culled list.
     */
    public static List<?> cull(Iterable<?> iterable, Iterable<Boolean> booleans) {
        if (iterable == null) return ImmutableList.of();
        if (booleans == null || Iterables.isEmpty(booleans)) return ImmutableList.copyOf(iterable);
        ImmutableList.Builder<Object> results = ImmutableList.builder();
        Iterator<?> booleanIterator = ((Iterable<?>) cycle(booleans)).iterator();
        for (Object object : iterable) {
            boolean keep = (Boolean) booleanIterator.next();
            if (keep) results.add(object);
        }
        return results.build();
    }

    /**
     * Filter a list so it only contains unique elements (no duplicates).
     *
     * @param iterable The list to filter.
     * @param key      Optionally, the lookup key that will be used to find distincts.
     * @return The filtered list.
     */
    public static List<?> distinct(Iterable<?> iterable, String key) {
        if (iterable == null) return ImmutableList.of();
        if (key != null) {
            key = key.trim().isEmpty() ? null : key;
        }
        Set<Integer> distinctKeys = new HashSet<>();
        ImmutableList.Builder<Object> b = ImmutableList.builder();
        for (Object object : iterable) {
            if (object == null) continue;
            final Integer hashCode;
            if (key == null) {
                hashCode = object.hashCode();
            } else {
                Object v = DataFunctions.lookup(object, key);
                hashCode = v == null ? null : v.hashCode();
            }
            if (hashCode != null && distinctKeys.contains(hashCode)) continue;
            distinctKeys.add(hashCode);
            b.add(object);
        }
        return b.build();
    }

    public static List<?> takeEvery(Iterable<?> iterable, long n) {
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

    /**
     * Return a list of distinct keys found in the given maps.
     * @param iterable The list of maps.
     * @return A list of keys.
     */
    public static List<?> keys(Iterable<?> iterable) {
        if (iterable == null) return ImmutableList.of();
        ImmutableSet.Builder<String> b = ImmutableSet.<String>builder();
        for (Object o : iterable) {
            if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String,?> m = (Map<String,?>) o;
                b.addAll(m.keySet());
            } else {
                b.addAll(ReflectionUtils.getProperties(o));
            }
        }
        return b.build().asList();
    }

    public static <K, V> Map<K, V> zipMap(Iterable<K> keys, Iterable<V> values) {
        if (keys == null || values == null) return ImmutableMap.of();
        ImmutableMap.Builder<K, V> b = ImmutableMap.builder();
        Iterator<K> keyIterator = keys.iterator();
        Iterator<V> valueIterator = values.iterator();
        while (keyIterator.hasNext() && valueIterator.hasNext()) {
            K key = keyIterator.next();
            V value = valueIterator.next();
            b.put(key, value);
        }
        return b.build();
    }

}
