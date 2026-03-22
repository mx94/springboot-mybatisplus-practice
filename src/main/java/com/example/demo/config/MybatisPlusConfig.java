package com.example.demo.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.example.demo.common.context.LoginUser;
import com.example.demo.common.context.UserContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis Plus 核心配置
 *
 * ============ 核心知识点：TenantLineInnerInterceptor ============
 *
 * MyBatis Plus 提供了一个"拦截器机制"，可以在 SQL 执行前自动修改它。
 * TenantLineInnerInterceptor 就是利用这个机制，
 * 在所有 SELECT / INSERT / UPDATE / DELETE 语句中，
 * 自动追加 AND tenant_id = ? 条件。
 *
 * 效果演示（你不需要写，插件自动做）：
 *   你写的查询：  SELECT * FROM customer WHERE status = 1
 *   实际执行的：  SELECT * FROM customer WHERE status = 1 AND tenant_id = 1001
 *
 * 这意味着：业务代码里完全不用关心租户问题，插件全权负责隔离！
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 不需要租户过滤的表（公共配置表，所有租户共享）
     * sys_user 也需要忽略，原因见 AuthInterceptor 的注释
     */
    private static final Set<String> IGNORE_TABLES =
            Set.of("tenant", "sys_role", "sys_user");

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // ① 租户隔离插件（必须放第一位！）
        interceptor.addInnerInterceptor(
                new TenantLineInnerInterceptor(new TenantLineHandler() {

                    /**
                     * 返回当前请求的租户 ID（从 ThreadLocal 中读取）
                     * JSqlParser 框架要求返回 Expression 类型，LongValue 是其 long 数值的实现
                     */
                    @Override
                    public Expression getTenantId() {
                        Long tenantId = UserContext.getTenantId();
                        return new LongValue(tenantId != null ? tenantId : 0L);
                    }

                    /**
                     * 数据库中租户字段的列名
                     */
                    @Override
                    public String getTenantIdColumn() {
                        return "tenant_id";
                    }

                    /**
                     * 【关键方法】决定哪些表"跳过"租户过滤
                     *
                     * 返回 true  → 该表不追加 tenant_id 条件（忽略）
                     * 返回 false → 该表追加 AND tenant_id = ? 条件（过滤）
                     */
                    @Override
                    public boolean ignoreTable(String tableName) {
                        // ① 公共表（没有 tenant_id 字段）直接忽略
                        if (IGNORE_TABLES.contains(tableName.toLowerCase())) {
                            return true;
                        }
                        LoginUser user = UserContext.get();
                        // ② 未登录（如登录接口、健康检查）→ 忽略
                        if (user == null) {
                            return true;
                        }
                        // ③ 超级管理员 → 忽略（他可以看所有租户数据）
                        if (user.isSuperAdmin()) {
                            return true;
                        }
                        // 其他情况：追加租户过滤
                        return false;
                    }
                })
        );

        // ② 分页插件（必须放在租户插件后面！否则分页 SQL 会被干扰）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
