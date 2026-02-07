package com.alex.market.mvc.config;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public class RedisTestconteinerConfig {
    @Container
    @ServiceConnection
    static final RedisContainer redisContainer =
            new RedisContainer(DockerImageName.parse("redis:7.4.2-bookworm"));
}
