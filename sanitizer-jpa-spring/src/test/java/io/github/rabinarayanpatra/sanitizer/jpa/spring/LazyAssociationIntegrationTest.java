package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

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
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListener;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(SanitizerJpaAutoConfiguration.class)
class LazyAssociationIntegrationTest {

	@Autowired
	TestEntityManager em;

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestConfig {
	}

	@Test
	void persistParentDoesNotInitializeLazyChildren() {
		final Department dept = new Department();
		dept.name = "  ENG  ";
		final Department persisted = em.persistFlushFind(dept);
		em.clear();
		final Department reloaded = em.find(Department.class, persisted.id);
		// children association is lazy and not touched; sanitization must succeed
		// without LazyInitializationException.
		reloaded.name = "  ENG-2  ";
		em.persistAndFlush(reloaded);
		assertEquals("eng-2", reloaded.name);
	}

	@Entity
	@EntityListeners(SanitizationEntityListener.class)
	static class Department implements Serializable {
		@Id
		@GeneratedValue
		Long id;
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
		@Sanitize(cascade = true)
		@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		List<Employee> employees = new ArrayList<>();
	}

	@Entity
	@EntityListeners(SanitizationEntityListener.class)
	static class Employee implements Serializable {
		@Id
		@GeneratedValue
		Long id;
		@Sanitize(using = {TrimSanitizer.class, UpperCaseSanitizer.class})
		String code;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "department_id")
		Department department;
	}
}
