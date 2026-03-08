package org.kirya343.api.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @NonNull
    private static final CacheControl IMAGE_CACHE = CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic();

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
            .addResourceLocations("file:./files/")
            .setCacheControl(IMAGE_CACHE)
            .setUseLastModified(true);

        registry.addResourceHandler("/images/**")
            .addResourceLocations("classpath:/images/")
            .setCacheControl(IMAGE_CACHE)
            .setUseLastModified(true);

        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(IMAGE_CACHE)
            .setUseLastModified(true);
    }
}
