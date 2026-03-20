package com.wjh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjh.entity.Friend;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友关系Mapper
 */
@Mapper
public interface FriendMapper extends BaseMapper<Friend> {
}
