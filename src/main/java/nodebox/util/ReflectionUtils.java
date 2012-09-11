package nodebox.util;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Locale.ENGLISH;

/**
 * Utilities for dealing with Java reflection.
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    public static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(ENGLISH) + s.substring(1);
    }

    public static String getterMethod(String field) {
        return "get" + capitalize(field);
    }

    public static Method getGetterMethod(Class<?> c, String field) {
        try {
            return c.getMethod(getterMethod(field));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Lookup a value through the getter method.
     *
     * @param o            The object to lookup. Cannot be null.
     * @param field        The field. If the field is "firstName", we look for a getter method "getFirstName". Cannot be null.
     * @param defaultValue The default value if the field could not be found. Can be null.
     * @return The value or the default value if no value could be found.
     */
    public static Object get(Object o, String field, Object defaultValue) {
        checkNotNull(o, "The given object cannot be null.");
        checkNotNull(field, "The field cannot be null.");
        try {
            Method m = getGetterMethod(o.getClass(), field);
            return m.invoke(o);
        } catch (Exception e) {
            return defaultValue;
        }
    }

}
