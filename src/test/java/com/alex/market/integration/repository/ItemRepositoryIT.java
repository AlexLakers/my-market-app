package com.alex.market.integration.repository;

import com.alex.market.config.PostgresTestconteinerConfig;
import com.alex.market.model.Item;
import com.alex.market.repository.ItemRepository;
import com.alex.market.search.ItemSort;
import com.alex.market.search.ItemSpecification;
import com.alex.market.search.SortColumn;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.stream.Stream;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(PostgresTestconteinerConfig.class)
@Transactional
@Sql(scripts = {
        "/sql/cleanup.sql",
        "/sql/data-test.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)

class ItemRepositoryIT {

    private final static Long VALID_ID=1000L;
    @Autowired
    private ItemRepository itemRepository;

    @ParameterizedTest
    @MethodSource("getArgs")
    void findAll_shouldReturnPageItemsByConditionsAndPagination(String search, int pageNumber, int pageSize, int expectedSize, Item expectedItem) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize, ItemSort.getOrderByPriceOrTitle(SortColumn.NO));

        Page<Item> actual = itemRepository.findAll(ItemSpecification.getSpecByTitleOrDescription(search), pageable);

        Assertions.assertThat(actual)
                .isNotNull()
                .hasSize(expectedSize)
                .contains(expectedItem);

    }

    @ParameterizedTest
    @MethodSource("getArgsSort")
    void findAll_shouldReturnPageItemsBySort(SortColumn sortColumn, Comparator<Item> givenComparator) {

        Pageable pageable = PageRequest.of(0, 3, ItemSort.getOrderByPriceOrTitle(sortColumn));

        Page<Item> actual = itemRepository.findAll(ItemSpecification.getSpecByTitleOrDescription(""), pageable);

        Assertions.assertThat(actual.getContent()).
                isSortedAccordingTo(givenComparator);
    }

    @Test
    void updateImagePath_shouldUpdateImagePath(){
        itemRepository.updateImagePathById(VALID_ID,"new_image_path");

        Assertions.assertThat(itemRepository.findById(VALID_ID).isPresent()).isTrue();
        Assertions.assertThat(itemRepository.findById(VALID_ID).get()).hasFieldOrPropertyWithValue("imgPath", "new_image_path");
    }

    static Stream<Arguments> getArgsSort() {

        Comparator<Item> comparator = Comparator.comparing(Item::getId);
        Comparator<Item> comparator2 = Comparator.comparing(Item::getTitle);
        Comparator<Item> comparator3 = Comparator.comparing(Item::getPrice);

        return Stream.of(
                Arguments.of(SortColumn.NO, comparator),
                Arguments.of(SortColumn.ALPHA, comparator2),
                Arguments.of(SortColumn.PRICE, comparator3)
        );
    }

    static Stream<Arguments> getArgs() {
        Item expectedItem = Item.builder()
                .id(1000L)
                .title("test-ball")
                .description("Test ball description")
                .imgPath("images/test-ball.jpg")
                .price(500L)
                .build();


        return Stream.of(
                Arguments.of("test", 0, 3, 3, expectedItem),
                Arguments.of("", 0, 3, 3, expectedItem),
                Arguments.of(null, 0, 3, 3, expectedItem),
                Arguments.of("ball", 0, 3, 1, expectedItem)
        );
    }


}