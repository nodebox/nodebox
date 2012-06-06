package nodebox.util;

import com.google.common.collect.Iterables;

import java.util.List;

public final class ListUtils {

    /**
     * Get the class of elements of the given list.
     * If a list is null, is empty, or has many different types, returns Object.class.
     *
     * @param objects The list to get.
     * @return the class of all items in the list or Object. Never null.
     */
    public static Class listClass(Iterable<?> objects) {
        if (objects == null) return Object.class;
        Class<?> c = classOfFirst(objects);
        return nestedListClass(objects, c);
    }

    private static Class nestedListClass(Iterable<?> objects, Class klass) {
        Class<?> c = klass;
        for (Object o : objects) {
            if (o == null) return Object.class;
            if (o instanceof List) {
                c = nestedListClass((Iterable) o, c);
                break;
            }
            while (c != Object.class) {
                if (! c.isAssignableFrom(o.getClass()))
                    c = c.getSuperclass();
                else
                    break;
            }
        }
        return c;
    }

    private static Class classOfFirst(Iterable<?> objects) {
        Object firstObject = Iterables.getFirst(objects, null);
        if (firstObject == null) return Object.class;
        if (firstObject instanceof List)
            return classOfFirst((Iterable) firstObject);
        return firstObject.getClass();
    }

}
