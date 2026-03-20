package com.wjh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjh.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
