package io.github.rabinarayanpatra.sanitizer.core;

import java.lang.reflect.Field;

/**
 * Pluggable gate that lets callers decide whether the traversal engine may
 * descend into a given field of a parent object. Used to avoid touching
 * non-initialized JPA lazy associations or other contexts where reading a field
 * is expensive or unsafe.
 *
 * @since 1.2.0
 */
@FunctionalInterface
public interface TraversalSafetyChecker {

	/**
	 * Returns true when the engine may read and recurse into {@code field} on
	 * {@code parent}.
	 *
	 * @param parent
	 *            the bean currently being walked
	 * @param field
	 *            the field about to be descended into
	 * @return true to allow descent; false to skip this field
	 */
	boolean shouldDescend(Object parent, Field field);

	/**
	 * No-op checker that always permits descent. Default used by
	 * {@code sanitizer-core} when callers do not supply a custom checker.
	 */
	TraversalSafetyChecker ALWAYS = (parent, field) -> true;
}
