package com.alex.market.mvc.service;

import com.alex.market.mvc.client.dto.PaymentRequest;
import com.alex.market.mvc.dto.output.*;
import com.alex.market.mvc.exception.OrderNotFoundException;
import com.alex.market.mvc.mapper.ItemMapper;
import com.alex.market.mvc.model.Item;
import com.alex.market.mvc.model.Order;
import com.alex.market.mvc.model.OrderItem;
import com.alex.market.mvc.model.OrderStatus;
import com.alex.market.mvc.repository.OrderItemRepository;
import com.alex.market.mvc.repository.OrderRepository;
import com.alex.market.mvc.repository.projection.OrderItemsDetails;
import com.alex.market.mvc.security.service.UserService;
import com.alex.market.mvc.security.service.UserServiceImpl;
import com.alex.market.mvc.service.impl.OrderServiceImpl;
import com.alex.market.mvc.service.impl.PaymentApiClientServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class OrderServiceTest {
    private final Long VALID_ID = 1L;
    private final Long INVALID_ID = 1000000L;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private UserService userService;
    @Autowired
    private PaymentApiClientService paymentApiClientService;

    private Map<Long, Integer> cartItemsCount;
    private ItemDto itemDto;
    private OrderItemsDetails orderItemsDetails;

    @BeforeEach
    void setUp() {
        cartItemsCount = new HashMap<>();
        cartItemsCount.put(VALID_ID, 1);
        itemDto = new ItemDto(VALID_ID, "testTitle1", "testDesc1", "testImagePath1", 1000L, 1);
        orderItemsDetails = new OrderItemsDetails(VALID_ID, itemDto.title(), itemDto.description(), itemDto.imgPath(), itemDto.price(), itemDto.count());
    }

    @Test
    void getAllPaidOrders_shouldReturnOrderDtoListSuccess() {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto), 1000L);
        when(orderRepository.findAllByStatus(OrderStatus.PAID)).thenReturn(Flux.fromIterable(List.of(Order.builder().id(VALID_ID).totalSum(1000L).build())));
        when(orderItemRepository.findItemsWithDetailsByOrderId(VALID_ID)).thenReturn(Flux.fromIterable(List.of(orderItemsDetails)));
        when(itemMapper.toDtoFromOrderItemDetails(orderItemsDetails)).thenReturn(itemDto);

        List<OrderDto> actualDto = orderService.findAllPaidOrders().collectList().block();

        assertThat(actualDto).hasSize(1).contains(orderDto);
    }

    @Test
    void findOrderWithItems_shouldReturnOrderDtoByIdSuccess() {
        OrderDto orderDto = new OrderDto(VALID_ID, List.of(itemDto), 1000L);
        Order order = Order.builder().id(VALID_ID).totalSum(1000L).build();
        when(orderRepository.findById(VALID_ID)).thenReturn(Mono.just(order));
        when(orderItemRepository.findItemsWithDetailsByOrderId(VALID_ID)).thenReturn(Flux.fromIterable(List.of(orderItemsDetails)));
        when(itemMapper.toDtoFromOrderItemDetails(orderItemsDetails)).thenReturn(itemDto);

        OrderDto actualOrderDto = orderService.findOrderWithItems(VALID_ID).block();

        Assertions.assertThat(actualOrderDto).isEqualTo(orderDto);
    }

    @Test
    void findOrderWithItems_shouldThrowOrderNotFoundException_whenMonoIsEmptyFail() {

        when(orderRepository.findById(INVALID_ID)).thenReturn(Mono.empty());
        when(orderItemRepository.findItemsWithDetailsByOrderId(INVALID_ID)).thenReturn(Flux.fromIterable(List.of(orderItemsDetails)));

        Assertions.assertThatExceptionOfType(OrderNotFoundException.class)
                .isThrownBy(() -> orderService.findOrderWithItems(INVALID_ID).block());
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1, SUCCESS,'', PAID",
            "1, 1, BUSINESS_FAIL,'Amount must be positive and account with id: 1', FAILED",
            "1, 1, NETWORK_ERROR,'', PENDING",
            "1, 1, SERVICE_ERROR, '','PENDING'"

    })
    void createAndProcessOrder_shouldCreateAndProcessOrderAndReturnDtoWithPaymentStatus(Long orderId, Long txId, String paymentStatus, String failureReasonFromApi, String orderStatus) {
        Item item = Item.builder().id(VALID_ID).price(1000L).title("title").description("descr").build();
        Order expectedOrder = Order.builder().id(orderId).build();
        PaymentDto paymentResponseDto = new PaymentDto(PaymentApiStatus.valueOf(paymentStatus), orderId, txId, failureReasonFromApi);
        OrderPaymentDto expectedPaymentDto = new OrderPaymentDto(orderId, orderStatus);
        List<OrderItem> listOrderItem = List.of(OrderItem.builder().itemId(VALID_ID).orderId(orderId).count(2).historyPrice(1000L).build());

        when(userService.getCurrentUserId()).thenReturn(Mono.just(VALID_ID));
        when(cartService.getItemsCartWithCounts(cartItemsCount)).thenReturn(Mono.just(Map.of(item, 2)));
        when(orderItemRepository.saveAll(Mockito.anyCollection())).thenReturn(Flux.fromIterable(listOrderItem));
        when(orderRepository.save(Mockito.any(Order.class))).thenReturn(Mono.just(expectedOrder));
        when(paymentApiClientService.processPaymentInTransaction(Mockito.any(PaymentRequest.class))).thenReturn(Mono.just(paymentResponseDto));

        StepVerifier.create(orderService.createAndProcessOrder(cartItemsCount))
                .expectNext(expectedPaymentDto)
                .verifyComplete();
    }

    @TestConfiguration
    static class OrderServiceTestContextConfiguration {
        @Bean
        public CartService cartService() {
            return Mockito.mock(CartService.class);
        }

        @Bean
        public PaymentApiClientService paymentApiClientService() {
            return Mockito.mock(PaymentApiClientServiceImpl.class);
        }

        @Bean
        public UserService userService() {
            return Mockito.mock(UserServiceImpl.class);
        }

        @Bean
        public OrderService orderService(PaymentApiClientService paymentApiClientService,
                                         OrderRepository orderRepository,
                                         OrderItemRepository orderItemRepository,
                                         ItemMapper itemMapper,
                                         CartService cartService,
                                         UserService userService) {
            return new OrderServiceImpl(orderRepository, orderItemRepository, itemMapper, cartService, paymentApiClientService, userService);
        }



        @Bean
        public OrderRepository orderRepository() {
            return Mockito.mock(OrderRepository.class);
        }

        @Bean
        public OrderItemRepository orderItemRepository() {
            return Mockito.mock(OrderItemRepository.class);
        }

        @Bean
        public ItemMapper itemMapper() {
            return Mockito.mock(ItemMapper.class);
        }
    }

}