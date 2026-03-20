package com.wjh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wjh.common.domain.PageResult;
import com.wjh.common.domain.dto.MessageSendDTO;
import com.wjh.common.domain.vo.ConversationVO;
import com.wjh.common.domain.vo.MessageVO;
import com.wjh.entity.Message;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService extends IService<Message> {

    /**
     * 保存单聊消息
     * @param fromUserId 发送者ID
     * @param sendDTO 发送参数
     * @return 保存后的消息
     */
    Message savePrivateMessage(Long fromUserId, MessageSendDTO sendDTO);

    /**
     * 查询会话历史
     * @param userId 当前用户ID
     * @param friendId 对方用户ID
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 分页消息
     */
    PageResult<MessageVO> getHistory(Long userId, Long friendId, Long pageNo, Long pageSize);

    /**
     * 查询最近会话
     * @param userId 当前用户ID
     * @param limit 返回条数
     * @return 会话列表
     */
    List<ConversationVO> getConversations(Long userId, Integer limit);

    /**
     * 标记某会话为已读
     * @param userId 当前用户ID
     * @param friendId 对方用户ID
     * @return 更新数量
     */
    int markRead(Long userId, Long friendId);

    /**
     * 按消息ID标记已读
     * @param userId 接收用户ID
     * @param msgId 消息ID
     */
    void markReadByMsgId(Long userId, Long msgId);

    /**
     * 获取离线未读消息
     * @param userId 用户ID
     * @param limit 限制条数
     * @return 消息列表
     */
    List<Message> listOfflineMessages(Long userId, Integer limit);
}
