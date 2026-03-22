package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.context.LoginUser;
import com.example.demo.common.context.UserContext;
import com.example.demo.entity.Customer;
import com.example.demo.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户接口（第二章验证用）
 *
 * ============ 验证租户隔离效果 ============
 *
 * 用不同的 X-User-Id 请求 /customer/list，观察控制台 SQL 和返回数据：
 *
 *   X-User-Id: 1 (超级管理员)  → SQL 无 tenant_id 过滤，返回全部 6 条客户
 *   X-User-Id: 2 (星云管理员)  → SQL 追加 AND tenant_id=1001，返回 4 条
 *   X-User-Id: 7 (海浪管理员)  → SQL 追加 AND tenant_id=1002，返回 2 条
 *
 * 注意："同一段代码"在不同用户上下文下产生了完全不同的 SQL！
 * 这就是租户插件的魔法所在。
 */
@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerMapper customerMapper;

    /**
     * 查询客户列表
     * 【核心验证点】：不同租户身份登录，自动返回不同数据！
     * 请观察控制台打印的 SQL 中 WHERE 条件的变化。
     */
    @GetMapping("/list")
    public Map<String, Object> list() {
        LoginUser currentUser = UserContext.get();

        // LambdaQueryWrapper：MyBatis Plus 提供的类型安全查询条件构造器
        // new LambdaQueryWrapper<Customer>() 表示"无自定义条件"
        // 但租户插件会在底层自动追加 AND tenant_id = ?
        List<Customer> customers = customerMapper.selectList(new LambdaQueryWrapper<Customer>());

        Map<String, Object> result = new HashMap<>();
        result.put("currentUser", currentUser != null ? currentUser.getRealName() : "匿名访问");
        result.put("role", currentUser != null ? currentUser.getRoleCode() : null);
        result.put("tenantId", currentUser != null ? currentUser.getTenantId() : null);
        result.put("count", customers.size());
        result.put("data", customers);
        return result;
    }

    /**
     * 查看当前登录用户信息（调试用）
     * 请求时携带 Header: X-User-Id: {任意用户ID}
     */
    @GetMapping("/whoami")
    public Object whoami() {
        LoginUser user = UserContext.get();
        if (user == null) {
            return "未登录（未携带 X-User-Id Header）";
        }
        return user;
    }
}
