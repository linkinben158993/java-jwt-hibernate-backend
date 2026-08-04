package io.linkinben.springbootsecurityjwt.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Applies all Flyway migrations (incl. V3 role seed) to a throwaway MySQL and asserts ROLE_USER +
 * ROLE_ADMIN exist. Requires a Docker daemon; skips (not fails) when Docker is unavailable.
 */
@Testcontainers
class RoleSeedIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Test
    void migrationsSeedBothRoles() throws Exception {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available - skipping Testcontainers IT");

        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Set<String> roles = new HashSet<>();
        try (Connection c = DriverManager.getConnection(
                     MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT rName FROM roles")) {
            while (rs.next()) roles.add(rs.getString("rName"));
        }
        assertThat(roles).contains("ROLE_USER", "ROLE_ADMIN");
    }
}
