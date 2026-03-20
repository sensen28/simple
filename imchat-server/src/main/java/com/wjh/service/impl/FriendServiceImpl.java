package com.wjh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wjh.common.domain.dto.FriendApplyDTO;
import com.wjh.common.domain.dto.FriendApplyHandleDTO;
import com.wjh.common.domain.vo.FriendApplyVO;
import com.wjh.common.domain.vo.FriendVO;
import com.wjh.common.enums.MessageTypeEnum;
import com.wjh.entity.Friend;
import com.wjh.entity.FriendApply;
import com.wjh.entity.User;
import com.wjh.mapper.FriendMapper;
import com.wjh.service.FriendApplyService;
import com.wjh.service.FriendService;
import com.wjh.service.UserService;
import com.wjh.service.WsPushService;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Resource
    private WsPushService wsPushService;

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
        apply.setRemark(StringUtils.hasText(applyDTO.getRemark()) ? applyDTO.getRemark().trim() : "");
        apply.setStatus(0);
        apply.setCreateTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());
        boolean save = friendApplyService.save(apply);
        if (save) {
            wsPushService.push(applyDTO.getTargetUserId(), MessageTypeEnum.FRIEND_APPLY_NOTICE, mapOf(
                    "applyId", apply.getId(),
                    "applyUserId", applyUserId,
                    "targetUserId", applyDTO.getTargetUserId(),
                    "remark", apply.getRemark(),
                    "status", 0
            ));
        }
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
        User applyUser = userService.getById(apply.getApplyUserId());
        User targetUser = userService.getById(targetUserId);
        // 同意申请，添加双向好友关系
        if (handleDTO.getOperateType() == 1) {
            ensureFriendRelation(targetUserId, apply.getApplyUserId());
            ensureFriendRelation(apply.getApplyUserId(), targetUserId);
        }
        wsPushService.push(apply.getApplyUserId(), MessageTypeEnum.FRIEND_STATUS_NOTICE, mapOf(
                "event", "FRIEND_APPLY_HANDLE",
                "applyId", apply.getId(),
                "operateType", handleDTO.getOperateType(),
                "targetUser", userMap(targetUser)
        ));
        wsPushService.push(targetUserId, MessageTypeEnum.FRIEND_STATUS_NOTICE, mapOf(
                "event", "FRIEND_APPLY_HANDLE",
                "applyId", apply.getId(),
                "operateType", handleDTO.getOperateType(),
                "applyUser", userMap(applyUser)
        ));
        if (handleDTO.getOperateType() == 1) {
            wsPushService.push(apply.getApplyUserId(), MessageTypeEnum.FRIEND_STATUS_NOTICE, mapOf(
                    "event", "FRIEND_ADDED",
                    "friend", userMap(targetUser)
            ));
            wsPushService.push(targetUserId, MessageTypeEnum.FRIEND_STATUS_NOTICE, mapOf(
                    "event", "FRIEND_ADDED",
                    "friend", userMap(applyUser)
            ));
        }
        return true;
    }

    @Override
    public List<FriendApplyVO> getFriendApplyList(Long targetUserId, Integer status) {
        LambdaQueryWrapper<FriendApply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FriendApply::getTargetUserId, targetUserId)
                .orderByDesc(FriendApply::getCreateTime);
        if (status != null) {
            queryWrapper.eq(FriendApply::getStatus, status);
        }
        List<FriendApply> applies = friendApplyService.list(queryWrapper);
        if (applies.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> applyUserIds = applies.stream()
                .map(FriendApply::getApplyUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userService.listByIds(applyUserIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return applies.stream().map(apply -> {
            FriendApplyVO vo = new FriendApplyVO();
            vo.setApplyId(apply.getId());
            vo.setApplyUserId(apply.getApplyUserId());
            User user = userMap.get(apply.getApplyUserId());
            if (user != null) {
                vo.setApplyUsername(user.getUsername());
                vo.setApplyNickname(user.getNickname());
                vo.setApplyAvatar(user.getAvatar());
            }
            vo.setRemark(apply.getRemark());
            vo.setStatus(apply.getStatus());
            vo.setCreateTime(apply.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
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
        wsPushService.push(friendId, MessageTypeEnum.FRIEND_STATUS_NOTICE, mapOf(
                "event", "FRIEND_DELETED",
                "friendId", userId
        ));
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
        boolean updated = updateById(friend);
        if (updated) {
            wsPushService.push(friendId, MessageTypeEnum.FRIEND_STATUS_NOTICE, mapOf(
                    "event", "FRIEND_BLOCK_UPDATED",
                    "operatorId", userId,
                    "status", status
            ));
        }
        return updated;
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
        boolean updated = updateById(friend);
        if (updated) {
            wsPushService.push(userId, MessageTypeEnum.FRIEND_STATUS_NOTICE, mapOf(
                    "event", "FRIEND_MUTE_UPDATED",
                    "friendId", friendId,
                    "isMute", isMute
            ));
        }
        return updated;
    }

    @Override
    public boolean isFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getStatus, 1);
        return count(queryWrapper) > 0;
    }

    private void ensureFriendRelation(Long userId, Long friendId) {
        LambdaQueryWrapper<Friend> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId);
        Friend relation = getOne(queryWrapper);
        if (relation != null) {
            relation.setStatus(1);
            relation.setIsMute(relation.getIsMute() == null ? 0 : relation.getIsMute());
            updateById(relation);
            return;
        }
        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus(1);
        friend.setIsMute(0);
        friend.setCreateTime(LocalDateTime.now());
        save(friend);
    }

    private Map<String, Object> userMap(User user) {
        Map<String, Object> result = new HashMap<>();
        if (user == null) {
            return result;
        }
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("status", user.getStatus());
        return result;
    }

    private Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
