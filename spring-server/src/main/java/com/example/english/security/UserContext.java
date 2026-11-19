package com.example.english.security;

import com.example.english.common.BizException;

/** 当前登录用户 id。拦截器写入，afterCompletion 必须清理，否则线程复用会串号。 */
public final class UserContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    public static Long get() {
        Long id = HOLDER.get();
        if (id == null) {
            throw new BizException(401, "未登录");
        }
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
