package com.guideconnect.config;

import org.springframework.context.annotation.Configuration;
<<<<<<< HEAD
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
=======
>>>>>>> 3229964df188615c94251e3acd655976dbec09b2
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for static view mappings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/error/400").setViewName("error/400");
        registry.addViewController("/error/403").setViewName("error/403");
        registry.addViewController("/error/404").setViewName("error/404");
        registry.addViewController("/error/500").setViewName("error/500");
    }
<<<<<<< HEAD

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps the URL path /images/tours/** 
        // to the physical folder on your computer
        registry.addResourceHandler("/images/tours/**")
                .addResourceLocations("file:src/main/resources/static/images/tours/");
    }
=======
>>>>>>> 3229964df188615c94251e3acd655976dbec09b2
}
