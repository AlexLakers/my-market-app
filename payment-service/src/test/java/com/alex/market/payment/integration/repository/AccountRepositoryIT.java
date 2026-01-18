package com.alex.market.payment.integration.repository;

import com.alex.market.payment.config.PostgresTestconteinerConfig;
import com.alex.market.payment.integration.TestDataLoader;
import com.alex.market.payment.repository.AccountRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataR2dbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ImportTestcontainers({PostgresTestconteinerConfig.class})
@ActiveProfiles("test")
class AccountRepositoryIT {

    @Autowired
    private AccountRepository accountRepository;


    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void setup() {
        TestDataLoader.loadTestData(databaseClient);
    }

    @ParameterizedTest
    @CsvSource({
            "5000, 1, 1",
            "10000000, 1, 0",
            "1000,-1,0"
    })
    void decrementBalanceAtomic_shouldUpdateBalanceAtomic_whenBalanceGreaterAmountAndAccountIdExits(Long amount,Long accountId,Long expectedRowsUpdated) {
        Long actualRowsUpdated = accountRepository.decrementBalanceAtomic(accountId, amount).block();

        Assertions.assertThat(actualRowsUpdated).isEqualTo(expectedRowsUpdated);
    }
}
