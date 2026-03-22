package com.example.demo.common.interceptor;

import com.example.demo.common.context.LoginUser;
import com.example.demo.common.context.UserContext;
import com.example.demo.entity.SysRole;
import com.example.demo.entity.SysUser;
import com.example.demo.mapper.SysRoleMapper;
import com.example.demo.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 身份认证拦截器
 *
 * ============ 工作原理 ============
 *
 * 每个 HTTP 请求到达 Controller 之前，都会先经过这里（preHandle）。
 * 我们从请求头（Header）中读取 "X-User-Id"，然后：
 *   1. 查询数据库获取用户信息
 *   2. 查询该用户的角色及数据权限范围
 *   3. 把这些信息存入 UserContext（ThreadLocal）
 *   4. 后续 Service/Controller 随时可以通过 UserContext.get() 拿到当前用户
 *
 * ============ 简化说明 ============
 * 真实项目中这里通常是解析 JWT Token（加密的身份令牌）。
 * 我们为了简化学习过程，直接传入 userId，效果完全等同。
 *
 * ============ 为什么要在 afterCompletion 清除 ============
 * Tomcat 是线程池模型，同一个线程会在不同请求间复用。
 * 如果不清除 ThreadLocal，下一个请求可能会拿到上一个用户的数据！
 * 这是个严重的安全漏洞，所以 clear() 必须调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor   // Lombok：自动为 final 字段生成构造器（Spring 推荐的注入方式）
public class AuthInterceptor implements HandlerInterceptor {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    /**
     * 请求到达 Controller 之前执行
     * 返回 true → 继续处理；返回 false → 直接中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isBlank()) {
            // 没有携带用户信息 → 匿名访问，放行
            return true;
        }

        try {
            Long userId = Long.parseLong(userIdStr);

            // 【重要】此时 UserContext 还是空，所以 MybatisPlusConfig 中
            // ignoreTable 对 sys_user 返回 true，不会加 tenant_id 过滤，
            // 才能用 userId 直接查到用户。这就是 sys_user 要加入 IGNORE_TABLES 的原因！
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                log.warn("[Auth] 用户 ID={} 不存在或已删除", userId);
                return true;
            }

            // 查询角色信息（sys_role 也在 IGNORE_TABLES 中，不受租户过滤）
            SysRole role = sysRoleMapper.selectById(user.getRoleId());

            // 构建登录用户上下文对象
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(user.getId());
            loginUser.setTenantId(user.getTenantId());
            loginUser.setDeptId(user.getDeptId());
            loginUser.setUsername(user.getUsername());
            loginUser.setRealName(user.getRealName());
            if (role != null) {
                loginUser.setDataScope(role.getDataScope());
                loginUser.setRoleCode(role.getCode());
            }

            // 存入 ThreadLocal，当前请求的整个调用链都能读取到
            UserContext.set(loginUser);
            log.debug("[Auth] 用户 [{}] 租户={} 角色={}", user.getRealName(), user.getTenantId(),
                    role != null ? role.getName() : "无");

        } catch (NumberFormatException e) {
            log.warn("[Auth] Header X-User-Id 格式错误: {}", userIdStr);
        }

        return true;
    }

    /**
     * 请求完全结束后执行（无论成功还是异常）
     * 必须清除 ThreadLocal，防止线程复用时数据串用
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
