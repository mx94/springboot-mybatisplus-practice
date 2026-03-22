package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户 Mapper
 *
 * 继承 BaseMapper<Customer> 后，所有 selectList / selectPage 等方法
 * 会被 MyBatis Plus 的租户插件自动拦截，追加 AND tenant_id = ? 条件。
 * 这意味着：业务代码完全不需要关心租户过滤，插件全权负责！
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
