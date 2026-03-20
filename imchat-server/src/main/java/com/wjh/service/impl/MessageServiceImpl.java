package com.wjh.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wjh.entity.Message;
import com.wjh.mapper.MessageMapper;
import com.wjh.service.MessageService;
import org.springframework.stereotype.Service;

/**
 * 消息服务实现类
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
}
