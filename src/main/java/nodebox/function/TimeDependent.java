package nodebox.function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java node function whose result can change between renders even when its inputs are the same,
 * because it reads the clock, the network or a device, or because it acts on the outside world. Such a
 * function runs on every render instead of being served from the render cache.
 *
 * @see Function#isTimeDependent()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TimeDependent {
}
