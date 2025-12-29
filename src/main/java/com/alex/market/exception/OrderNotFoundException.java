package com.alex.market.exception;

public class OrderNotFoundException extends RuntimeException{
    public OrderNotFoundException(Long id) {
        this("The order with id: {%d} is not found".formatted(id));
    }
    public OrderNotFoundException(Long id,Throwable cause) {
        this("The order with id: {%d} is not found".formatted(id),cause);
    }
    public OrderNotFoundException(String message) {
        super(message);
    }
    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
