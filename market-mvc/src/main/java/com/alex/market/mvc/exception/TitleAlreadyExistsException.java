package com.alex.market.mvc.exception;

public class TitleAlreadyExistsException extends RuntimeException {
    public TitleAlreadyExistsException(String title) {
        super("The title: %1$s is already exists".formatted(title));
    }

    public TitleAlreadyExistsException(String title, Throwable cause) {
        super("The title: %1$s is already exists".formatted(title), cause);
    }


}
