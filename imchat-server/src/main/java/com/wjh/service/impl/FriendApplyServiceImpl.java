package com.wjh.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wjh.entity.FriendApply;
import com.wjh.mapper.FriendApplyMapper;
import com.wjh.service.FriendApplyService;
import org.springframework.stereotype.Service;

/**
 * 好友申请服务实现类
 */
@Service
public class FriendApplyServiceImpl extends ServiceImpl<FriendApplyMapper, FriendApply> implements FriendApplyService {
}
