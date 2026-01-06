package com.alex.market.repository;

import com.alex.market.config.PostgresTestconteinerConfig;
import com.alex.market.model.Order;
import com.alex.market.repository.projection.OrderItemsDetails;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataR2dbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ImportTestcontainers({PostgresTestconteinerConfig.class})
@ActiveProfiles("test")
class OrderItemRepositoryTest {

    private static Long VALID_ID=1000L;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Test
    void findItemsWithDetailsByOrderId(){
        List<OrderItemsDetails> expectedListDetails=List.of(new OrderItemsDetails(VALID_ID,"test1-ball","Test ball description1","images/test-ball.jpg",500L,2));
        List<OrderItemsDetails> actualListDetails=orderItemRepository.findItemsWithDetailsByOrderId(VALID_ID).collectList().block();

        Assertions.assertThat(actualListDetails)
                .hasSize(1)
                .contains(expectedListDetails.getFirst());
    }
}