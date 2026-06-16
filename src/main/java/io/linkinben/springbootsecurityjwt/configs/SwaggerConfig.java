package io.linkinben.springbootsecurityjwt.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("An's API")
                        .description("Elearning API")
                        .version("3rd Version")
                        .contact(new Contact()
                                .name("An")
                                .email("thienan.nguyenhoang311@gmail.com")));
    }
}
