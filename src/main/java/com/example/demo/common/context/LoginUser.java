package com.example.demo.common.context;

import lombok.Data;

/**
 * 当前登录用户的信息对象
 * 通过 UserContext（ThreadLocal）在每次请求中传递
 */
@Data
public class LoginUser {

    /** 用户ID */
    private Long userId;

    /** 所属租户ID（超级管理员为 0，其余均为实际租户ID） */
    private Long tenantId;

    /** 所属部门ID */
    private Long deptId;

    /** 登录用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /**
     * 数据范围（来自角色表的 data_scope 字段）
     *   1 = 全部数据（平台超管）
     *   2 = 本租户所有数据
     *   3 = 本部门数据
     *   4 = 仅自己负责的数据
     */
    private Integer dataScope;

    /** 角色编码 */
    private String roleCode;

    /** 是否是平台超级管理员 */
    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(this.roleCode);
    }
}
