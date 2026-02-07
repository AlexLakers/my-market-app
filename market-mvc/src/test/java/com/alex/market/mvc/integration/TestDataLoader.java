package com.alex.market.mvc.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.r2dbc.core.DatabaseClient;


public class TestDataLoader {

    public static void loadTestData(DatabaseClient databaseClient) {
        databaseClient.sql("DELETE FROM orders_items").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM orders").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM items").fetch().rowsUpdated().block();

        databaseClient.sql("SELECT setval(pg_get_serial_sequence('items', 'id'), 1002)")
                .fetch().rowsUpdated().block();
        databaseClient.sql("SELECT setval(pg_get_serial_sequence('orders', 'id'), 1002)")
                .fetch().rowsUpdated().block();
        databaseClient.sql("SELECT setval(pg_get_serial_sequence('orders_items', 'id'), 1002)")
                .fetch().rowsUpdated().block();

        databaseClient.sql("""
                INSERT INTO items (id, title, description, img_path, price) VALUES
                (1000, 'test1-ball', 'Test ball description1', 'images/test-ball.jpg', 500),
                (1001, 'test1-car', 'Test car description1', 'images/test-car.jpg', 50000),
                (1002, 'test-computer', 'Test computer description', 'images/test-computer.jpg', 15000)
                """).fetch().rowsUpdated().block();

        databaseClient.sql("""
                INSERT INTO orders (id, total_sum,status) VALUES
                (1000, 1000,'PAID'),
                (1001, 50000,'PAID'),
                (1002, 45000,'PAID')
                """).fetch().rowsUpdated().block();

        databaseClient.sql("""
                INSERT INTO orders_items (item_id, order_id, history_price, count) VALUES
                (1000, 1000, 500, 2),
                (1001, 1001, 50000, 1),
                (1002, 1002, 15000, 3)
                """).fetch().rowsUpdated().block();
    }
}
