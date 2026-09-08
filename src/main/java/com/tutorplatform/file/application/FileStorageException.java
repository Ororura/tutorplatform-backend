package com.tutorplatform.file.application;

public class FileStorageException extends RuntimeException {
    public FileStorageException(Throwable cause) {
        super("File storage operation failed", cause);
    }
}
