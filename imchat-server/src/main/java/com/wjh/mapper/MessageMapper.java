package com.wjh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjh.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
