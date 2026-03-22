package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * 【知识点：BaseMapper<T>】
 * MyBatis Plus 提供的基础 Mapper 接口，只需继承它就自动拥有：
 *   insert / deleteById / updateById / selectById
 *   selectList / selectPage / selectCount
 * 这些常用 CRUD 方法，全部无需手写 SQL！
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // 暂时不需要自定义方法
    // 后续如果需要复杂的关联查询，可以在这里添加 @Select 注解方法或对应的 XML 文件
}
