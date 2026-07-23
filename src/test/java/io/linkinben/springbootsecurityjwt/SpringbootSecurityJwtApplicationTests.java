package io.linkinben.springbootsecurityjwt;

import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SpringbootSecurityJwtApplicationTests {

	// Mock the @Repository impls that carry @PersistenceContext EntityManager.
	// Without these, PersistenceAnnotationBeanPostProcessor would fail because
	// DataSource/JPA auto-configuration is excluded in application-test.yml.
	@MockitoBean UserRepositoryImpl userRepository;
	@MockitoBean RoleRepositoryImpl roleRepository;

	@Test
	void contextLoads() {
	}

}
