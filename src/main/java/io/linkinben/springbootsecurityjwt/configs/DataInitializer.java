package io.linkinben.springbootsecurityjwt.configs;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.services.RoleService;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds ROLE_USER and ROLE_ADMIN on startup when running the dev profile.
 *
 * TODO (low priority): replace with a Flyway or Liquibase migration so seeding
 * happens at the DB level and works in all environments, not just dev.
 */
@Slf4j
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final List<String> REQUIRED_ROLES = List.of("ROLE_USER", "ROLE_ADMIN");

    @Autowired
    private RoleService roleService;

    @Override
    public void run(String... args) {
        Set<String> existing = roleService.findAll()
                .stream()
                .map(Roles::getrName)
                .collect(Collectors.toSet());

        for (String roleName : REQUIRED_ROLES) {
            if (!existing.contains(roleName)) {
                Roles role = new Roles();
                role.setrName(roleName);
                roleService.add(role);
                log.info("Seeded role: {}", roleName);
            } else {
                log.debug("Role already exists, skipping: {}", roleName);
            }
        }
    }
}
