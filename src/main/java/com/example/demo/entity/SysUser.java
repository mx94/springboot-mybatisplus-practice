package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户实体（对应 sys_user 表）
 *
 * 【知识点：@TableLogic（软删除）】
 * 标注了 @TableLogic 的字段，MyBatis Plus 会在：
 *   - SELECT 时：自动追加 AND deleted = 0（只查未删除的）
 *   - DELETE 时：自动转为 UPDATE ... SET deleted = 1（标记删除而非真正删除）
 * 这样数据不会真正丢失，出了问题还能找回。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID */
    private Long tenantId;

    /** 所属部门ID */
    private Long deptId;

    /** 角色ID（关联 sys_role 表） */
    private Long roleId;

    /** 登录用户名 */
    private String username;

    /** 密码（实际项目中应存 BCrypt 加密后的值） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 软删除标记：0=正常，1=已删除 */
    @TableLogic
    private Integer deleted;
}
