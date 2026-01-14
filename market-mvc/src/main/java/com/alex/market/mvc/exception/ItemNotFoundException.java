package com.alex.market.mvc.exception;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(Long id) {
        this("The item with id: {%d} is not found".formatted(id));
    }
    public ItemNotFoundException(Long id,Throwable cause) {
        this("The item with id: {%d} is not found".formatted(id),cause);
    }
    public ItemNotFoundException(String message) {
        super(message);
    }
    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
