package io.github.rabinarayanpatra.sanitizer.core;

import org.junit.jupiter.api.Test;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TruncateSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanitizationUtilsTest {

	@Test
	void apply_sanitizesAnnotatedFields() {
		final MutableBean bean = new MutableBean();
		bean.name = "  HELLO  ";
		SanitizationUtils.apply(bean);
		assertEquals("hello", bean.name);
	}

	@Test
	void apply_skipsNullBean() {
		// Should not throw
		SanitizationUtils.apply(null);
	}

	@Test
	void apply_skipsNullFieldValue() {
		final MutableBean bean = new MutableBean();
		bean.name = null;
		// Should not throw — sanitizers handle null internally
		SanitizationUtils.apply(bean);
		assertNull(bean.name);
	}

	@Test
	void apply_throwsOnRecord() {
		final ImmutableRecord rec = new ImmutableRecord("  HELLO  ");
		final UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
				() -> SanitizationUtils.apply(rec));
		assertTrue(ex.getMessage().contains("ImmutableRecord"));
		assertTrue(ex.getMessage().contains("records"));
	}

	// --- P0: @Repeatable support ---

	@Test
	void apply_repeatableSanitizeAnnotations() {
		final RepeatableBean bean = new RepeatableBean();
		bean.name = "  HELLO  ";
		SanitizationUtils.apply(bean);
		assertEquals("hello", bean.name);
	}

	// --- P0: Superclass field walking ---

	@Test
	void apply_sanitizesSuperclassFields() {
		final ChildBean bean = new ChildBean();
		bean.parentName = "  PARENT  ";
		bean.childName = "  CHILD  ";
		SanitizationUtils.apply(bean);
		assertEquals("parent", bean.parentName);
		assertEquals("child", bean.childName);
	}

	@Test
	void apply_sanitizesDeepHierarchy() {
		final GrandchildBean bean = new GrandchildBean();
		bean.parentName = "  GRANDPARENT  ";
		bean.childName = "  MIDDLE  ";
		bean.grandchildName = "  LEAF  ";
		SanitizationUtils.apply(bean);
		assertEquals("grandparent", bean.parentName);
		assertEquals("middle", bean.childName);
		assertEquals("leaf", bean.grandchildName);
	}

	// --- P0: Type-safety guard ---

	@Test
	void apply_throwsOnTypeMismatch() {
		final TypeMismatchBean bean = new TypeMismatchBean();
		bean.count = 42;
		final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> SanitizationUtils.apply(bean));
		assertTrue(ex.getMessage().contains("Type mismatch"));
		assertTrue(ex.getMessage().contains("count"));
		assertTrue(ex.getCause() instanceof ClassCastException);
	}

	@Test
	void apply_writesBackWhenSanitizerReturnsNonNullForNullInput() {
		final DefaultingBean bean = new DefaultingBean();
		bean.value = null;
		SanitizationUtils.apply(bean);
		assertEquals("DEFAULT", bean.value);
	}

	@Test
	void apply_configurableSanitizerWithBlankParamsSkipsConfigure() {
		// Covers the `!ann.params().isBlank()` false side of the compound branch.
		final BlankParamsBean bean = new BlankParamsBean();
		bean.text = "hello";
		SanitizationUtils.apply(bean);
		// Default maxLength (255) applies → input unchanged.
		assertEquals("hello", bean.text);
	}

	@Test
	void apply_throwsInstantiationExceptionForPrivateConstructorSanitizer() {
		final PrivateCtorBean bean = new PrivateCtorBean();
		bean.value = "x";
		final SanitizerInstantiationException ex = assertThrows(SanitizerInstantiationException.class,
				() -> SanitizationUtils.apply(bean));
		assertTrue(ex.getMessage().contains("Cannot instantiate sanitizer"));
		assertNotNull(ex.getCause());
	}

	// --- Test fixtures ---

	static class MutableBean {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
	}

	static class RepeatableBean {
		@Sanitize(using = TrimSanitizer.class)
		@Sanitize(using = LowerCaseSanitizer.class)
		String name;
	}

	static class ParentBean {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String parentName;
	}

	static class ChildBean extends ParentBean {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String childName;
	}

	static class GrandchildBean extends ChildBean {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String grandchildName;
	}

	static class TypeMismatchBean {
		@Sanitize(using = TrimSanitizer.class)
		Integer count;
	}

	record ImmutableRecord(@Sanitize(using = TrimSanitizer.class) String name) {
	}

	static class DefaultingBean {
		@Sanitize(using = DefaultingSanitizer.class)
		String value;
	}

	public static class DefaultingSanitizer implements FieldSanitizer<String> {
		public DefaultingSanitizer() {
		}

		@Override
		public @Nullable String sanitize(final @Nullable String input) {
			return input == null ? "DEFAULT" : input;
		}
	}

	static class BlankParamsBean {
		@Sanitize(using = TruncateSanitizer.class, params = "")
		String text;
	}

	static class PrivateCtorBean {
		@Sanitize(using = PrivateCtorSanitizer.class)
		String value;
	}

	public static class PrivateCtorSanitizer implements FieldSanitizer<String> {
		// No no-arg constructor → ReflectiveOperationException during instantiation.
		@SuppressWarnings("UnusedMethod")
		public PrivateCtorSanitizer(final String unused) {
		}

		@Override
		public @Nullable String sanitize(final @Nullable String input) {
			return input;
		}
	}
}
