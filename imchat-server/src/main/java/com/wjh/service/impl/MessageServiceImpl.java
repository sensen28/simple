package com.wjh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wjh.common.domain.PageResult;
import com.wjh.common.domain.dto.MessageSendDTO;
import com.wjh.common.domain.vo.ConversationVO;
import com.wjh.common.domain.vo.MessageVO;
import com.wjh.entity.Message;
import com.wjh.entity.User;
import com.wjh.mapper.MessageMapper;
import com.wjh.service.MessageService;
import com.wjh.service.UserService;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 消息服务实现类
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Resource
    private UserService userService;

    @Override
    public Message savePrivateMessage(Long fromUserId, MessageSendDTO sendDTO) {
        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(sendDTO.getToUserId());
        message.setContent(sendDTO.getContent());
        message.setType(sendDTO.getType() == null ? 1 : sendDTO.getType());
        message.setStatus(0);
        message.setIsEncrypted(1);
        message.setCreateTime(LocalDateTime.now());
        save(message);
        return message;
    }

    @Override
    public PageResult<MessageVO> getHistory(Long userId, Long friendId, Long pageNo, Long pageSize) {
        long current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        long size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Page<Message> page = new Page<>(current, size);
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                        .and(inner -> inner.eq(Message::getFromUserId, userId).eq(Message::getToUserId, friendId))
                        .or(inner -> inner.eq(Message::getFromUserId, friendId).eq(Message::getToUserId, userId)))
                .orderByDesc(Message::getCreateTime);
        page(page, queryWrapper);
        List<MessageVO> records = page.getRecords().stream()
                .sorted(Comparator.comparing(Message::getCreateTime))
                .map(this::toMessageVO)
                .collect(Collectors.toList());
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ConversationVO> getConversations(Long userId, Integer limit) {
        int maxConversations = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        int scanLimit = Math.max(maxConversations * 30, 200);
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.eq(Message::getFromUserId, userId).or().eq(Message::getToUserId, userId))
                .orderByDesc(Message::getCreateTime)
                .last("limit " + scanLimit);
        List<Message> messages = list(queryWrapper);
        if (CollectionUtils.isEmpty(messages)) {
            return new ArrayList<>();
        }

        Map<Long, Message> latestMessageMap = new LinkedHashMap<>();
        for (Message message : messages) {
            Long friendId = Objects.equals(message.getFromUserId(), userId) ? message.getToUserId() : message.getFromUserId();
            latestMessageMap.putIfAbsent(friendId, message);
            if (latestMessageMap.size() >= maxConversations) {
                break;
            }
        }
        List<Long> friendIds = new ArrayList<Long>(latestMessageMap.keySet());
        Map<Long, User> userMap = userService.listByIds(friendIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        LambdaQueryWrapper<Message> unreadQuery = new LambdaQueryWrapper<>();
        unreadQuery.eq(Message::getToUserId, userId)
                .eq(Message::getStatus, 0)
                .in(Message::getFromUserId, friendIds);
        Map<Long, Long> unreadCountMap = list(unreadQuery).stream()
                .collect(Collectors.groupingBy(Message::getFromUserId, Collectors.counting()));

        return friendIds.stream().map(friendId -> {
            Message latest = latestMessageMap.get(friendId);
            User friend = userMap.get(friendId);
            ConversationVO vo = new ConversationVO();
            vo.setFriendId(friendId);
            if (friend != null) {
                vo.setFriendUsername(friend.getUsername());
                vo.setFriendNickname(friend.getNickname());
                vo.setFriendAvatar(friend.getAvatar());
            }
            vo.setLastContent(latest != null ? latest.getContent() : "");
            vo.setLastTime(latest != null ? latest.getCreateTime() : null);
            vo.setUnreadCount(unreadCountMap.getOrDefault(friendId, 0L));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public int markRead(Long userId, Long friendId) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getToUserId, userId)
                .eq(Message::getFromUserId, friendId)
                .eq(Message::getStatus, 0);
        List<Message> unreadMessages = list(queryWrapper);
        if (CollectionUtils.isEmpty(unreadMessages)) {
            return 0;
        }
        unreadMessages.forEach(message -> message.setStatus(1));
        updateBatchById(unreadMessages);
        return unreadMessages.size();
    }

    @Override
    public void markReadByMsgId(Long userId, Long msgId) {
        lambdaUpdate()
                .set(Message::getStatus, 1)
                .eq(Message::getId, msgId)
                .eq(Message::getToUserId, userId)
                .update();
    }

    @Override
    public List<Message> listOfflineMessages(Long userId, Integer limit) {
        int offlineLimit = limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Message::getToUserId, userId)
                .eq(Message::getStatus, 0)
                .orderByAsc(Message::getCreateTime)
                .last("limit " + offlineLimit);
        return list(queryWrapper);
    }

    private MessageVO toMessageVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setMsgId(message.getId());
        vo.setFromUserId(message.getFromUserId());
        vo.setToUserId(message.getToUserId());
        vo.setContent(message.getContent());
        vo.setType(message.getType());
        vo.setStatus(message.getStatus());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
