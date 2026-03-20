package com.wjh.controller;

import com.wjh.common.domain.PageResult;
import com.wjh.common.domain.Result;
import com.wjh.common.domain.dto.MessageSendDTO;
import com.wjh.common.domain.vo.ConversationVO;
import com.wjh.common.domain.vo.MessageVO;
import com.wjh.common.enums.MessageTypeEnum;
import com.wjh.entity.Message;
import com.wjh.service.FriendService;
import com.wjh.service.MessageService;
import com.wjh.service.WsPushService;
import com.wjh.utils.SecurityUtils;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private MessageService messageService;

    @Resource
    private FriendService friendService;

    @Resource
    private WsPushService wsPushService;

    /**
     * 发送单聊消息（HTTP兜底）
     */
    @PostMapping("/send")
    public Result<MessageVO> sendMessage(@Valid @RequestBody MessageSendDTO sendDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!friendService.isFriend(userId, sendDTO.getToUserId())) {
            return Result.error(400, "对方不是你的好友，无法发送消息");
        }
        Message message = messageService.savePrivateMessage(userId, sendDTO);
        MessageVO vo = new MessageVO();
        vo.setMsgId(message.getId());
        vo.setFromUserId(message.getFromUserId());
        vo.setToUserId(message.getToUserId());
        vo.setContent(message.getContent());
        vo.setType(message.getType());
        vo.setStatus(message.getStatus());
        vo.setCreateTime(message.getCreateTime());

        Map<String, Object> wsPayload = new HashMap<String, Object>();
        wsPayload.put("msgId", message.getId());
        wsPayload.put("fromUserId", message.getFromUserId());
        wsPayload.put("toUserId", message.getToUserId());
        wsPayload.put("content", message.getContent());
        wsPayload.put("type", message.getType());
        wsPayload.put("timestamp", System.currentTimeMillis());
        wsPushService.push(sendDTO.getToUserId(), MessageTypeEnum.PRIVATE_MESSAGE, wsPayload);
        return Result.success("发送成功", vo);
    }

    /**
     * 获取消息历史
     */
    @GetMapping("/history/{friendId}")
    public Result<PageResult<MessageVO>> getHistory(@PathVariable Long friendId,
                                                    @RequestParam(defaultValue = "1") Long pageNo,
                                                    @RequestParam(defaultValue = "50") Long pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!friendService.isFriend(userId, friendId)) {
            return Result.error(400, "对方不是你的好友");
        }
        PageResult<MessageVO> history = messageService.getHistory(userId, friendId, pageNo, pageSize);
        return Result.success(history);
    }

    /**
     * 获取最近会话
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations(@RequestParam(defaultValue = "20") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ConversationVO> conversations = messageService.getConversations(userId, limit);
        return Result.success(conversations);
    }

    /**
     * 标记会话消息已读
     */
    @PostMapping("/read/{friendId}")
    public Result<Integer> markRead(@PathVariable Long friendId) {
        Long userId = SecurityUtils.getCurrentUserId();
        int count = messageService.markRead(userId, friendId);
        return Result.success("操作成功", count);
    }
}
