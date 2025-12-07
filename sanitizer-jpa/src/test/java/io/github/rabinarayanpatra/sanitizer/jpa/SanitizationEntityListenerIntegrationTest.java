package io.github.rabinarayanpatra.sanitizer.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.CreditCardMaskSanitizer;
import io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListenerIntegrationTest.TestConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestConfig.class)
class SanitizationEntityListenerIntegrationTest {

	@Autowired
	PaymentRepository repository;

	@Test
	void entityIsSanitizedBeforePersist() {
		final PaymentEntity p = new PaymentEntity();
		p.setCardNumber("1234-5678-9012-3456");
		final PaymentEntity saved = repository.save(p);

		// Should be masked to "**** **** **** 3456"
		assertThat(saved.getCardNumber()).endsWith("3456");
		assertThat(saved.getCardNumber()).startsWith("****");
	}

	interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
	}

	@SpringBootApplication(scanBasePackageClasses = SanitizationEntityListenerIntegrationTest.class)
	@EnableJpaRepositories(basePackageClasses = SanitizationEntityListenerIntegrationTest.class, considerNestedRepositories = true // ←
																																	// enable
																																	// nested
																																	// scanning
	)
	@EntityScan(basePackageClasses = SanitizationEntityListenerIntegrationTest.class)
	static class TestConfig {
		// Nothing else needed: this will
		// 1) Auto-configure H2 + Spring Data JPA,
		// 2) Scan nested @Entity and nested JpaRepository,
		// 3) Pick up your SanitizationEntityListener bean.
	}

	@Entity(name = "payment")
	@EntityListeners(SanitizationEntityListener.class)
	static class PaymentEntity {
		@Id
		@GeneratedValue
		Long id;

		@Sanitize(using = CreditCardMaskSanitizer.class)
		private String cardNumber;

		// getters/setters…
		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getCardNumber() {
			return cardNumber;
		}

		public void setCardNumber(final String cn) {
			this.cardNumber = cn;
		}
	}
}
