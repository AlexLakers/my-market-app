package com.alex.market.mvc.exception;

public class ImageStorageException extends RuntimeException{
    public ImageStorageException(String path) {
        super("Image storage error: %1$s ".formatted(path));
    }
    public ImageStorageException(String path, Throwable cause) {
        super("Image storage error: %1$s ".formatted(path), cause);
    }
}
