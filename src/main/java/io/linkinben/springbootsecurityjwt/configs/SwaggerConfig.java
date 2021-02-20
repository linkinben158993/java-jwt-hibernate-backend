package io.linkinben.springbootsecurityjwt.configs;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
	@SuppressWarnings("unchecked")
	private ApiInfo apiInfo() {
		String title = "An's API";
		String description = "Elearning API";
		String version = "2nd Version";
		String termsOfServiceUrl = "Not Available";
		Contact contactName = new Contact("An", "", "thienan.nguyenhoang311@gmail.com");
		String license = "Not Available!";
		String licenseUrl = "Not Available!";
		return new ApiInfo(title, description, version, termsOfServiceUrl, contactName, license, licenseUrl,
				Collections.EMPTY_LIST);
	}

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.basePackage("io.linkinben.springbootsecurityjwt.controllers"))
				.paths(PathSelectors.any()).build()
				.apiInfo(apiInfo());
	}
}
