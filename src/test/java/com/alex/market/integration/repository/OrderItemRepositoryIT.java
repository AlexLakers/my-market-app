package com.alex.market.integration.repository;

import com.alex.market.config.PostgresTestconteinerConfig;
import com.alex.market.repository.OrderItemRepository;
import com.alex.market.repository.projection.OrderItemsDetails;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@DataR2dbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ImportTestcontainers({PostgresTestconteinerConfig.class})
@ActiveProfiles("test")
class OrderItemRepositoryIT {

    private static Long VALID_ID = 1000L;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void findItemsWithDetailsByOrderId_shouldReturnListOrderItemsDetailsNotEmpty() {
        List<OrderItemsDetails> actualListDetails = orderItemRepository.findItemsWithDetailsByOrderId(VALID_ID).collectList().block();

        Assertions.assertThat(actualListDetails)
                .hasSize(1);
        Assertions.assertThat(actualListDetails.getFirst())
                .hasFieldOrPropertyWithValue("id", VALID_ID)
                .hasFieldOrPropertyWithValue("title", "test1-ball")
                .hasFieldOrPropertyWithValue("price", 500L)
                .hasFieldOrPropertyWithValue("count", 2);
    }
}