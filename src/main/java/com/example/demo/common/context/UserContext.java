package com.example.demo.common.context;

/**
 * 用户上下文工具类（基于 ThreadLocal）
 *
 * ============ 核心知识点：ThreadLocal ============
 *
 * ThreadLocal 为每个线程维护"独立的变量副本"。
 * Web 服务器（如 Tomcat）每个 HTTP 请求使用独立线程处理，
 * 所以同时有 100 个请求，就有 100 个独立的 ThreadLocal 副本，互不干扰。
 *
 * 【生命周期】
 *   1. 请求进来 → AuthInterceptor.preHandle() → UserContext.set(loginUser)
 *   2. 请求处理中 → Controller/Service 里调用 UserContext.get() 读取用户信息
 *   3. 请求结束 → AuthInterceptor.afterCompletion() → UserContext.clear()
 *
 * ⚠️ 重要：Tomcat 使用线程池，线程会被下一个请求复用。
 *    如果不执行 clear()，下一个请求会获取到上一个请求残留的用户信息！
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> USER_HOLDER = new ThreadLocal<>();

    /** 存入当前用户 */
    public static void set(LoginUser user) {
        USER_HOLDER.set(user);
    }

    /** 获取当前用户（请求未携带用户信息时返回 null） */
    public static LoginUser get() {
        return USER_HOLDER.get();
    }

    /**
     * 请求结束后必须调用，释放 ThreadLocal 持有的引用。
     * 用 remove() 而不是 set(null)，因为 remove() 才会真正清除 Entry。
     */
    public static void clear() {
        USER_HOLDER.remove();
    }

    /** 快捷方法：获取当前租户ID */
    public static Long getTenantId() {
        LoginUser user = get();
        return user != null ? user.getTenantId() : null;
    }
}
