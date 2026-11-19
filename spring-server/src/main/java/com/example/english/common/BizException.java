package com.example.english.common;

public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(String msg) {
        this(4001, msg);
    }

    public int getCode() {
        return code;
    }
}
