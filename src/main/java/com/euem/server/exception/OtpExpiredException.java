package com.euem.server.exception;

public class OtpExpiredException extends RuntimeException {
    
    public OtpExpiredException(String message) {
        super(message);
    }
    
    public OtpExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
