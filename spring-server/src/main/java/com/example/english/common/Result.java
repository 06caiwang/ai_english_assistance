package com.example.english.common;

/**
 * 统一返回结构。
 * <p>业务错误一律 HTTP 200 + code != 0，只有鉴权失败用 HTTP 401，
 * 方便客户端拦截器统一跳登录页。
 */
public record Result<T>(int code, String msg, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
