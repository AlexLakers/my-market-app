package com.alex.market.payment.exception;

public class AccountNotFoundException extends RuntimeException{
        public AccountNotFoundException(Long id) {
            this("The account with id: {%d} is not found".formatted(id));
        }
        public AccountNotFoundException(Long id,Throwable cause) {
            this("The account with id: {%d} is not found".formatted(id),cause);
        }
        public AccountNotFoundException(String message) {
            super(message);
        }
        public AccountNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
