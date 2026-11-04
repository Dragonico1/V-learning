package com.elearning.platform.config;

import com.elearning.platform.services.PortadaService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Todos los controladores de la aplicación cuelgan de /api (el proxy de Vite apunta ahí). */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PortadaService portadas;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forBasePackage("com.elearning.platform.controller"));
    }

    /** Sirve las portadas subidas como archivos estáticos. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = portadas.directorio().toUri().toString();
        if (!base.endsWith("/")) base += "/";
        registry.addResourceHandler("/api/archivos/portadas/**")
                .addResourceLocations(base);
    }
}
