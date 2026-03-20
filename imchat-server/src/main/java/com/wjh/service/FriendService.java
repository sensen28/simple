package com.wjh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wjh.common.domain.dto.FriendApplyDTO;
import com.wjh.common.domain.dto.FriendApplyHandleDTO;
import com.wjh.common.domain.vo.FriendApplyVO;
import com.wjh.common.domain.vo.FriendVO;
import com.wjh.entity.Friend;

import java.util.List;

/**
 * 好友服务接口
 */
public interface FriendService extends IService<Friend> {

    /**
     * 发送好友申请
     * @param applyUserId 申请人ID
     * @param applyDTO 申请参数
     * @return 是否成功
     */
    boolean applyFriend(Long applyUserId, FriendApplyDTO applyDTO);

    /**
     * 处理好友申请
     * @param targetUserId 目标用户ID（被申请人）
     * @param handleDTO 处理参数
     * @return 是否成功
     */
    boolean handleFriendApply(Long targetUserId, FriendApplyHandleDTO handleDTO);

    /**
     * 获取好友申请列表
     * @param targetUserId 目标用户ID
     * @param status 申请状态（可为空）
     * @return 好友申请列表
     */
    List<FriendApplyVO> getFriendApplyList(Long targetUserId, Integer status);

    /**
     * 获取好友列表
     * @param userId 用户ID
     * @return 好友列表
     */
    List<FriendVO> getFriendList(Long userId);

    /**
     * 删除好友
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否成功
     */
    boolean deleteFriend(Long userId, Long friendId);

    /**
     * 拉黑/取消拉黑好友
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param status 状态：0-拉黑，1-正常
     * @return 是否成功
     */
    boolean blockFriend(Long userId, Long friendId, Integer status);

    /**
     * 设置好友免打扰
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param isMute 是否免打扰：0-提醒，1-免打扰
     * @return 是否成功
     */
    boolean setMute(Long userId, Long friendId, Integer isMute);

    /**
     * 检查是否是好友
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否是好友
     */
    boolean isFriend(Long userId, Long friendId);
}
