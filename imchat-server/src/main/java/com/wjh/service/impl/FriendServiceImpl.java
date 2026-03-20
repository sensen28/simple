package com.wjh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wjh.common.domain.dto.FriendApplyDTO;
import com.wjh.common.domain.dto.FriendApplyHandleDTO;
import com.wjh.common.domain.vo.FriendVO;
import com.wjh.entity.Friend;
import com.wjh.entity.FriendApply;
import com.wjh.entity.User;
import com.wjh.mapper.FriendMapper;
import com.wjh.service.FriendApplyService;
import com.wjh.service.FriendService;
import com.wjh.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 好友服务实现类
 */
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {

    @Resource
    private FriendApplyService friendApplyService;

    @Resource
    private UserService userService;

    @Override
    public boolean applyFriend(Long applyUserId, FriendApplyDTO applyDTO) {
        // 不能添加自己为好友
        if (applyUserId.equals(applyDTO.getTargetUserId())) {
            throw new RuntimeException("不能添加自己为好友");
        }
        // 检查是否已经是好友
        if (isFriend(applyUserId, applyDTO.getTargetUserId())) {
            throw new RuntimeException("对方已经是你的好友");
        }
        // 检查是否已经发送过申请
        LambdaQueryWrapper<FriendApply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendApply::getApplyUserId, applyUserId)
                .eq(FriendApply::getTargetUserId, applyDTO.getTargetUserId())
                .eq(FriendApply::getStatus, 0);
        if (friendApplyService.count(queryWrapper) > 0) {
            throw new RuntimeException("已发送过好友申请，请等待对方处理");
        }
        // 创建好友申请
        FriendApply apply = new FriendApply();
        apply.setApplyUserId(applyUserId);
        apply.setTargetUserId(applyDTO.getTargetUserId());
        apply.setRemark(applyDTO.getRemark());
        apply.setStatus(0);
        apply.setCreateTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());
        boolean save = friendApplyService.save(apply);
        // TODO 发送通知给目标用户
        return save;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleFriendApply(Long targetUserId, FriendApplyHandleDTO handleDTO) {
        // 查询申请记录
        FriendApply apply = friendApplyService.getById(handleDTO.getApplyId());
        if (apply == null) {
            throw new RuntimeException("申请记录不存在");
        }
        // 校验是否是申请的目标用户
        if (!apply.getTargetUserId().equals(targetUserId)) {
            throw new RuntimeException("无权处理该申请");
        }
        // 检查申请状态
        if (apply.getStatus() != 0) {
            throw new RuntimeException("该申请已被处理");
        }
        // 更新申请状态
        apply.setStatus(handleDTO.getOperateType());
        apply.setUpdateTime(LocalDateTime.now());
        friendApplyService.updateById(apply);
        // 同意申请，添加双向好友关系
        if (handleDTO.getOperateType() == 1) {
            // 添加申请人到目标用户的好友列表
            Friend friend1 = new Friend();
            friend1.setUserId(targetUserId);
            friend1.setFriendId(apply.getApplyUserId());
            friend1.setStatus(1);
            friend1.setIsMute(0);
            friend1.setCreateTime(LocalDateTime.now());
            save(friend1);
            // 添加目标用户到申请人的好友列表
            Friend friend2 = new Friend();
            friend2.setUserId(apply.getApplyUserId());
            friend2.setFriendId(targetUserId);
            friend2.setStatus(1);
            friend2.setIsMute(0);
            friend2.setCreateTime(LocalDateTime.now());
            save(friend2);
            // TODO 发送通知给申请人
        }
        return true;
    }

    @Override
    public List<FriendVO> getFriendList(Long userId) {
        // 查询好友列表
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, 1);
        List<Friend> friends = list(queryWrapper);
        if (friends.isEmpty()) {
            return new ArrayList<>();
        }
        // 查询好友信息
        List<Long> friendIds = friends.stream().map(Friend::getFriendId).collect(Collectors.toList());
        List<User> users = userService.listByIds(friendIds);
        // 构造返回结果
        return users.stream().map(user -> {
            FriendVO friendVO = new FriendVO();
            friendVO.setFriendId(user.getId());
            friendVO.setUsername(user.getUsername());
            friendVO.setNickname(user.getNickname());
            friendVO.setAvatar(user.getAvatar());
            // 获取备注
            friends.stream()
                    .filter(f -> f.getFriendId().equals(user.getId()))
                    .findFirst()
                    .ifPresent(f -> {
                        friendVO.setRemark(f.getRemark());
                        friendVO.setIsMute(f.getIsMute());
                    });
            friendVO.setStatus(user.getStatus());
            return friendVO;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFriend(Long userId, Long friendId) {
        // 删除双向好友关系
        LambdaQueryWrapper<Friend> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        remove(wrapper1);
        LambdaQueryWrapper<Friend> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId);
        remove(wrapper2);
        return true;
    }

    @Override
    public boolean blockFriend(Long userId, Long friendId, Integer status) {
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        Friend friend = getOne(queryWrapper);
        if (friend == null) {
            throw new RuntimeException("好友不存在");
        }
        friend.setStatus(status);
        return updateById(friend);
    }

    @Override
    public boolean setMute(Long userId, Long friendId, Integer isMute) {
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        Friend friend = getOne(queryWrapper);
        if (friend == null) {
            throw new RuntimeException("好友不存在");
        }
        friend.setIsMute(isMute);
        return updateById(friend);
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getStatus, 1);
        return count(queryWrapper) > 0;
    }
}
