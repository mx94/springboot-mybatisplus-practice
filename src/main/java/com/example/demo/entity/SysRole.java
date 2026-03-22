package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体（对应 sys_role 表）
 *
 * 【知识点：@TableName】
 *   当类名和表名不一致时，用 @TableName 指定表名。
 *   MyBatis Plus 默认会把驼峰类名转为下划线表名（SysRole → sys_role），
 *   所以其实这个注解可以省略，但显式写出来更清晰。
 */
@Data
@TableName("sys_role")
public class SysRole {

    /**
     * 【知识点：@TableId】
     * 标注主键字段。IdType.ASSIGN_ID = 雪花算法（框架自动生成 Long 型唯一 ID）
     * 在 insert 时不需要手动设置 id，MyBatis Plus 会自动赋值。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 角色编码（程序里用这个判断权限，如 SUPER_ADMIN） */
    private String code;

    /** 角色名称（显示给用户看） */
    private String name;

    /**
     * 数据范围（这是数据权限的核心字段！）
     *   1 = 全部数据   （SUPER_ADMIN）
     *   2 = 本租户数据 （TENANT_ADMIN）
     *   3 = 本部门数据 （DEPT_MANAGER）
     *   4 = 仅本人数据 （SALESPERSON）
     */
    private Integer dataScope;
}
