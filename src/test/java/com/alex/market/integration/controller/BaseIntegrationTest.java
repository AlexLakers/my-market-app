package com.alex.market.integration.controller;

import com.alex.market.config.PostgresTestconteinerConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@AutoConfigureMockMvc
@Transactional
@ImportTestcontainers(PostgresTestconteinerConfig .class)
@Sql(scripts = {
        "/sql/cleanup.sql",
        "/sql/data-test.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BaseIntegrationTest {
}
