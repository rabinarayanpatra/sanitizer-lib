package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.hibernate5.SpringBeanContainer;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

import io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListener;

/**
 * Spring Boot auto-configuration that exposes a {@link HibernateSafetyChecker}
 * and configures Hibernate to instantiate {@link SanitizationEntityListener}
 * through Spring's {@link SpringBeanContainer}. The listener picks up the
 * checker as a setter-injected dependency.
 *
 * @since 1.2.0
 */
@AutoConfiguration
@ConditionalOnClass({EntityManagerFactory.class, BeanContainer.class})
public class SanitizerJpaAutoConfiguration {

	// SharedEntityManagerCreator is referenced to ensure Spring's JPA support is on
	// the classpath at compile time; it is not used at runtime here.
	@SuppressWarnings("unused")
	private static final Class<?> SHARED = SharedEntityManagerCreator.class;

	@Bean
	@ConditionalOnMissingBean
	public HibernateSafetyChecker hibernateSafetyChecker(final EntityManagerFactory emf) {
		final PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
		return new HibernateSafetyChecker(util);
	}

	@Bean
	@ConditionalOnBean(HibernateSafetyChecker.class)
	public SanitizationEntityListener sanitizationEntityListener(final HibernateSafetyChecker checker) {
		final SanitizationEntityListener listener = new SanitizationEntityListener();
		listener.setSafetyChecker(checker);
		return listener;
	}

	@Bean
	public HibernatePropertiesCustomizer sanitizerSpringBeanContainerCustomizer(
			final ConfigurableListableBeanFactory beanFactory) {
		return properties -> properties.put(AvailableSettings.BEAN_CONTAINER, new SpringBeanContainer(beanFactory));
	}
}
