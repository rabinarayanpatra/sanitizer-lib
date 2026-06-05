package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldDescriptorTest {

	@Test
	void pojoFieldDescriptorExposesFieldAndChain() throws NoSuchFieldException {
		final Field f = Fixtures.class.getDeclaredField("name");
		final List<FieldSanitizer<Object>> chain = List.of(new TrimSanitizer().asObjectSanitizer());
		final FieldDescriptor d = FieldDescriptor.forPojoField(f, chain, false, Kind.LEAF, null);
		assertSame(f, d.field());
		assertNull(d.recordComponent());
		assertEquals(chain, d.chain());
		assertEquals(Kind.LEAF, d.kind());
		assertNull(d.elementType());
		assertTrue(!d.cascade());
	}

	static class Fixtures {
		String name;
	}
}
