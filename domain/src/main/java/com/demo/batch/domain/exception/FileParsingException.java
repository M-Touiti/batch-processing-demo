package com.demo.batch.domain.exception;
public class FileParsingException extends RuntimeException {
    public FileParsingException(String message) { super(message); }
    public FileParsingException(String message, Throwable cause) { super(message, cause); }
}
