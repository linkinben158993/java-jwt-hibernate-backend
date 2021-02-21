package io.linkinben.springbootsecurityjwt.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMVCConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/uploads/**")
				.addResourceLocations("file:///C:/LinkinBen/I.T/Intern-SideProject/file-uploads/");

		registry.addResourceHandler("/static/assets/**").addResourceLocations("classpath:/static/assets/");

		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");

		registry.addResourceHandler("/webjars/**").addResourceLocations("/webjars/");
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// Local debugging
		registry.addMapping("/**").allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH")
				.allowedOrigins("http://localhost:4200").allowedHeaders("*").allowedMethods("*").allowCredentials(true);
		registry.addMapping("**/swagger-ui.html/**").allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH");
		registry.addMapping("/api/**").allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH");
	}
}
