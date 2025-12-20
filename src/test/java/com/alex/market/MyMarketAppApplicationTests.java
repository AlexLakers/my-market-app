package com.alex.market;

import com.alex.market.config.PostgresTestconteinerConfig;
import com.alex.market.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@ImportTestcontainers(PostgresTestconteinerConfig.class)
@Transactional
class MyMarketAppApplicationTests {

	@Test
	void contextLoads() {

	}

}
