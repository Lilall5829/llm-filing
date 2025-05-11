package com.example.filing.exception;

/**
 * 业务异常类，用于处理业务逻辑异常
 */
public class BusinessException extends RuntimeException {

    private int code;
    private String message;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}