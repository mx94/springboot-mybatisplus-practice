package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户实体（对应 customer 表）
 *
 * 【核心字段说明】
 *   tenant_id → 租户隔离（MyBatis Plus 插件自动处理）
 *   dept_id   → 部门经理权限过滤用
 *   owner_id  → 销售员权限过滤用
 *
 * 同一张表同一个查询接口，通过这三个字段的组合过滤，
 * 实现了"租户/部门/个人"三个层级的数据权限控制。
 */
@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户（多租户隔离的核心字段，由 MyBatis Plus 插件自动追加到 SQL） */
    private Long tenantId;

    /** 所属部门（部门经理数据范围过滤） */
    private Long deptId;

    /** 负责销售员的用户ID（销售员数据范围过滤） */
    private Long ownerId;

    /** 客户名称 */
    private String name;

    /** 联系电话 */
    private String phone;

    /** 所在行业 */
    private String industry;

    /**
     * 客户状态
     *   1 = 跟进中
     *   2 = 已成交
     *   3 = 已流失
     */
    private Integer status;

    /** 软删除标记 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;
}
