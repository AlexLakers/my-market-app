package com.alex.market.exception;

public class TitleAlreadyExistsException extends RuntimeException {
    public TitleAlreadyExistsException(String title) {
        super("The title: %1$s".formatted(title));
    }

    public TitleAlreadyExistsException(String title, Throwable cause) {
        super("The title: %1$s".formatted(title), cause);
    }


}
