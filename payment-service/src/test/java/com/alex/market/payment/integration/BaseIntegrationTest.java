package com.alex.market.payment.integration;


import com.alex.market.payment.config.PostgresTestconteinerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ImportTestcontainers(PostgresTestconteinerConfig.class)
@ActiveProfiles("test")
public class BaseIntegrationTest {

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void setup() {
        TestDataLoader.loadTestData(databaseClient);
    }
}
