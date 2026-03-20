package com.wjh.controller;

import com.wjh.common.domain.Result;
import com.wjh.common.domain.dto.FriendApplyDTO;
import com.wjh.common.domain.dto.FriendApplyHandleDTO;
import com.wjh.common.domain.vo.FriendApplyVO;
import com.wjh.common.domain.vo.FriendVO;
import com.wjh.service.FriendService;
import com.wjh.utils.SecurityUtils;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友控制器
 */
@Validated
@RestController
@RequestMapping("/friend")
public class FriendController {

    @Resource
    private FriendService friendService;

    /**
     * 发送好友申请
     */
    @PostMapping("/apply")
    public Result<Boolean> applyFriend(@Valid @RequestBody FriendApplyDTO applyDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = friendService.applyFriend(userId, applyDTO);
        return Result.success("申请已发送", success);
    }

    /**
     * 处理好友申请
     */
    @PostMapping("/apply/handle")
    public Result<Boolean> handleFriendApply(@Valid @RequestBody FriendApplyHandleDTO handleDTO) {
        if (handleDTO.getOperateType() == null || (handleDTO.getOperateType() != 1 && handleDTO.getOperateType() != 2)) {
            return Result.error(400, "操作类型不合法");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = friendService.handleFriendApply(userId, handleDTO);
        return Result.success("处理成功", success);
    }

    /**
     * 获取好友申请列表
     */
    @GetMapping("/apply/list")
    public Result<List<FriendApplyVO>> getApplyList(@RequestParam(required = false) Integer status) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<FriendApplyVO> list = friendService.getFriendApplyList(userId, status);
        return Result.success(list);
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result<List<FriendVO>> getFriendList() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<FriendVO> list = friendService.getFriendList(userId);
        return Result.success(list);
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public Result<Boolean> deleteFriend(@PathVariable Long friendId) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = friendService.deleteFriend(userId, friendId);
        return Result.success("删除成功", success);
    }

    /**
     * 拉黑/取消拉黑
     */
    @PutMapping("/{friendId}/block")
    public Result<Boolean> blockFriend(@PathVariable Long friendId, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态值不合法");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = friendService.blockFriend(userId, friendId, status);
        return Result.success("操作成功", success);
    }

    /**
     * 设置免打扰
     */
    @PutMapping("/{friendId}/mute")
    public Result<Boolean> setMute(@PathVariable Long friendId, @RequestParam Integer isMute) {
        if (isMute == null || (isMute != 0 && isMute != 1)) {
            return Result.error(400, "参数值不合法");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = friendService.setMute(userId, friendId, isMute);
        return Result.success("设置成功", success);
    }
}
