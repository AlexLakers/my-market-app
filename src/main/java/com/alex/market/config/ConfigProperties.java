package com.alex.market.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "market.upload")
public class ConfigProperties {
    private Path dir=Path.of("/my-market");
    private int maxSize=5242880;
}
