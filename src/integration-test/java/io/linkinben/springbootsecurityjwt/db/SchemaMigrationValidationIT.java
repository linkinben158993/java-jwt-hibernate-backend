package io.linkinben.springbootsecurityjwt.db;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The real migration guard: boots the JPA layer with Hibernate {@code ddl-auto=validate} + Flyway against
 * a throwaway MySQL. If the migrated schema doesn't match the entities (e.g. wrong physical column names),
 * the EntityManagerFactory fails to build and this test FAILS — which is exactly what a plain
 * Flyway-apply-then-JDBC test misses. Also confirms {@code V3} seeded the two roles.
 *
 * <p>Crucially this uses the app's real Spring Boot naming strategy (via {@code @DataJpaTest}), so it
 * reproduces the exact {@code validate} the running app performs. Requires Docker; the class is skipped
 * (not failed) when Docker is unavailable.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class SchemaMigrationValidationIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate"); // the whole point
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false"); // fresh DB → run V1..V3
    }

    @Autowired
    private EntityManager entityManager;

    @Test
    void migratedSchemaValidatesAgainstEntities_andRolesSeeded() {
        // Reaching here at all means the EntityManagerFactory built → Hibernate `validate` PASSED against
        // the Flyway-migrated schema (columns/types match every entity). Then confirm the V3 seed ran:
        List<String> roleNames = entityManager
                .createQuery("select r.rName from roles r", String.class)
                .getResultList();
        assertThat(roleNames).contains("ROLE_USER", "ROLE_ADMIN");
    }
}
