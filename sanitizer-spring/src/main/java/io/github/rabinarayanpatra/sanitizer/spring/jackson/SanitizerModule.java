package io.github.rabinarayanpatra.sanitizer.spring.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

/**
 * Jackson module that integrates with Spring Boot to apply
 * {@link io.github.rabinarayanpatra.sanitizer.annotation.Sanitize} annotations
 * during JSON deserialization.
 * <p>
 * This module wraps default bean deserializers and invokes
 * {@link SanitizationUtils#applyAndReturn(Object)} immediately after a bean is
 * fully deserialized. For mutable POJOs the same instance is returned with
 * fields mutated in place; for records the engine reconstructs a new instance
 * with sanitized components and returns it in place of the raw deserialized
 * record.
 *
 * @since 1.0.0
 */
public final class SanitizerModule extends SimpleModule {

	/**
	 * Creates a new instance of the {@code SanitizerModule}, registering a custom
	 * deserializer modifier that applies sanitization logic to deserialized beans.
	 */
	public SanitizerModule() {
		super("SanitizerModule");

		setDeserializerModifier(new MyBeanDeserializerModifier());
	}

	/**
	 * Delegating deserializer that applies {@link SanitizationUtils} after the
	 * default deserialization process.
	 */
	private static class SanitizingDeserializer extends DelegatingDeserializer {

		/**
		 * Constructs a new {@code SanitizingDeserializer} that delegates to the given
		 * deserializer.
		 *
		 * @param delegate
		 *            the original deserializer to wrap
		 */
		protected SanitizingDeserializer(final JsonDeserializer<?> delegate) {
			super(delegate);
		}

		/**
		 * Deserializes the input and then applies sanitization logic to the resulting
		 * object.
		 *
		 * @param p
		 *            the JSON parser
		 * @param ctxt
		 *            the deserialization context
		 * @return the sanitized object
		 * @throws IOException
		 *             if deserialization fails
		 */
		@Override
		public Object deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
			final Object bean = super.deserialize(p, ctxt);
			return SanitizationUtils.applyAndReturn(bean);
		}

		/**
		 * Creates a new delegating instance wrapping a different underlying
		 * deserializer.
		 *
		 * @param newDelegate
		 *            the new delegate to use
		 * @return a new {@code SanitizingDeserializer}
		 */
		@Override
		protected JsonDeserializer<?> newDelegatingInstance(final JsonDeserializer<?> newDelegate) {
			return new SanitizingDeserializer(newDelegate);
		}
	}

	private static class MyBeanDeserializerModifier extends BeanDeserializerModifier {
		@Override
		public JsonDeserializer<?> modifyDeserializer(final DeserializationConfig config,
				final BeanDescription beanDesc, final JsonDeserializer<?> deserializer) {
			return new SanitizingDeserializer(deserializer);
		}
	}
}
