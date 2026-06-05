package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Compiled descriptor for a single field (POJO) or record component. Built once
 * per class by {@link ClassMetadata} and reused on every traversal.
 */
record FieldDescriptor(@Nullable Field field, @Nullable RecordComponent recordComponent,
		List<FieldSanitizer<Object>> chain, boolean cascade, Kind kind, @Nullable Class<?> elementType) {

	static FieldDescriptor forPojoField(final Field field, final List<FieldSanitizer<Object>> chain,
			final boolean cascade, final Kind kind, final @Nullable Class<?> elementType) {
		return new FieldDescriptor(field, null, chain, cascade, kind, elementType);
	}

	static FieldDescriptor forRecordComponent(final RecordComponent component, final List<FieldSanitizer<Object>> chain,
			final boolean cascade, final Kind kind, final @Nullable Class<?> elementType) {
		return new FieldDescriptor(null, component, chain, cascade, kind, elementType);
	}
}
