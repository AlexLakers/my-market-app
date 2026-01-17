package com.alex.market.payment.integration;

import org.springframework.r2dbc.core.DatabaseClient;


public class TestDataLoader {

    public static void loadTestData(DatabaseClient databaseClient) {
        databaseClient.sql("DELETE FROM transactions").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM accounts").fetch().rowsUpdated().block();

        databaseClient.sql("SELECT setval(pg_get_serial_sequence('accounts', 'id'), 1002)")
                .fetch().rowsUpdated().block();
        databaseClient.sql("SELECT setval(pg_get_serial_sequence('transactions', 'id'), 1002)")
                .fetch().rowsUpdated().block();

        databaseClient.sql("""
                INSERT INTO accounts (id, balance, user_id) VALUES
                (0, 0, 100),
                (1, 500000, 1)
                """).fetch().rowsUpdated().block();

        databaseClient.sql("""
                INSERT INTO transactions (id, account_id,order_id,amount,status,type,failure_reason) VALUES
                (1000, 1,1,10000,'SUCCESS','PAYMENT',NULL),
                (1001, 0,2,100,'FAILED','PAYMENT','Only account with id=1 is supported')
                """).fetch().rowsUpdated().block();
    }
}
