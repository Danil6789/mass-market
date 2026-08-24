package com.marketplace.product.images.exception;

/**
 * Thrown when the filesystem cannot store an uploaded image
 * (mkdir failure, IO error, disk full). Mapped to HTTP 500.
 */
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}