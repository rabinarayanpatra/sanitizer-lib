package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(SanitizerJpaAutoConfiguration.class)
class SanitizerJpaAutoConfigurationTest {

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestConfig {
	}

	@Autowired
	TestEntityManager em;

	@Autowired
	HibernateSafetyChecker checker;

	@Test
	void contextWiresHibernateSafetyCheckerBean() {
		assertNotNull(checker);
	}

	@Test
	void persistRunsListenerAndSanitizesTopLevelField() {
		final UserEntity u = new UserEntity();
		u.email = "  USER@EXAMPLE.COM  ";
		final UserEntity saved = em.persistFlushFind(u);
		assertEquals("user@example.com", saved.email);
	}

	@Entity
	@EntityListeners(SanitizationEntityListener.class)
	static class UserEntity implements Serializable {
		@Id
		@GeneratedValue
		Long id;
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String email;
	}
}
