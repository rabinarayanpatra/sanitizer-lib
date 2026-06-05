package io.github.rabinarayanpatra.sanitizer.core.traversal;

/**
 * Classification of a field's declared type that drives traversal behavior.
 */
enum Kind {
	/**
	 * Strings, primitives/boxed, UUID, JSR-310 temporals, BigDecimal/Integer, Enum.
	 */
	LEAF,
	/**
	 * Plain mutable Java object (any class that is not LEAF/RECORD/COLLECTION/MAP).
	 */
	POJO,
	/** Subclass of {@link java.lang.Record}. */
	RECORD,
	/** Subtype of {@link java.util.Collection}. */
	COLLECTION,
	/** Subtype of {@link java.util.Map}. */
	MAP
}
