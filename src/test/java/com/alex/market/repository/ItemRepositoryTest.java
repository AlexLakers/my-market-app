package com.alex.market.repository;

import com.alex.market.config.PostgresTestconteinerConfig;
import com.alex.market.model.Item;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@DataR2dbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ImportTestcontainers({PostgresTestconteinerConfig.class})
@ActiveProfiles("test")
class ItemRepositoryTest {

    private static final Long MIN_PRICE=500L;

    @Autowired
    private ItemRepository itemRepository;

    @ParameterizedTest
    @CsvSource({
            "description1, 2",
            "test1, 2"
    })
    void findAll_shouldReturnPageWithContentBySearch(String search,int expectedSize) {

        var pageable = PageRequest.of(0, expectedSize, Sort.by("price"));
        Page<Item> page = itemRepository.findAll(search, pageable)
                .block();
        Assertions.assertThat(page).isNotNull();
        Assertions.assertThat(page.getContent()).first().hasFieldOrPropertyWithValue(Item.Fields.price,MIN_PRICE);
        Assertions.assertThat(page.getTotalElements()).isEqualTo(expectedSize);
        Assertions.assertThat(page.getNumber()).isEqualTo(0);
        Assertions.assertThat(page.getSize()).isEqualTo(expectedSize);
        Assertions.assertThat(page.hasNext()).isFalse();
        Assertions.assertThat(page.hasPrevious()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void findAll_shouldReturnPageWithAllContent(String givenSearch) {
        var pageable = PageRequest.of(0, 2, Sort.by("price"));
        Page<Item> page = itemRepository.findAll(givenSearch, pageable)
                .block();

        Assertions.assertThat(page).isNotNull();
        Assertions.assertThat(page.getContent()).first().hasFieldOrPropertyWithValue(Item.Fields.price,MIN_PRICE);
        Assertions.assertThat(page.getTotalElements()).isEqualTo(3);
        Assertions.assertThat(page.getNumber()).isEqualTo(0);
        Assertions.assertThat(page.getSize()).isEqualTo(2);
        Assertions.assertThat(page.hasNext()).isTrue();
        Assertions.assertThat(page.hasPrevious()).isFalse();
    }
}