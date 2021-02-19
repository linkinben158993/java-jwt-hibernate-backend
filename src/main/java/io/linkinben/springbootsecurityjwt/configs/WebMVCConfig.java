package io.linkinben.springbootsecurityjwt.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMVCConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/uploads/**").addResourceLocations("file:///C:/LinkinBen/I.T/Intern-SideProject/file-uploads/");

		registry.addResourceHandler("/static/assets/**").addResourceLocations("classpath:/static/assets/");
	}

}
