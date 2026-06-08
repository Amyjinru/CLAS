package com.clas.config;

import com.clas.entity.User;

public class UserContext {
    private static final ThreadLocal<User> THREAD_LOCAL = new ThreadLocal<>();

    public static void setUser(User user) {
        THREAD_LOCAL.set(user);
    }

    public static User getUser() {
        return THREAD_LOCAL.get();
    }

    public static String getUserId() {
        User user = THREAD_LOCAL.get();
        return user != null ? user.getPhone() : null;
    }

    public static String getRole() {
        User user = THREAD_LOCAL.get();
        return user != null ? user.getRole() : null;
    }

    public static void clear() {
        THREAD_LOCAL.remove();
    }
}
