package com.alex.market.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.resource.PathResourceResolver;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    private ConfigProperties configProperties;

    public WebFluxConfig(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path imagesDir = configProperties.getDir().resolve("images");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + imagesDir.toAbsolutePath() + "/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Mono<Resource> getResource(String resourcePath,
                                                         Resource location) {
                        try {
                            Resource resource = location.createRelative(resourcePath);
                            if (resource.exists() && resource.isReadable()) {
                                return Mono.just(resource);
                            } else {
                                return Mono.empty();
                            }
                        } catch (Exception e) {
                            return Mono.empty();
                        }
                    }
                });
    }
}
