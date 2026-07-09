package com.ws.bitesmart.controller;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.UserProfile;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康档案接口
 *
 * 用户填写问卷式的健康档案，系统根据档案内容做 AI 推荐。
 * 每个用户只有一份，GET 查、PUT 改。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 获取当前用户的健康档案
     * GET /api/user/profile
     */
    @GetMapping
    public ResultVO<UserProfile> getProfile(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        UserProfile profile = userProfileService.getByUserId(loginUser.getUserId());
        return ResultVO.success(profile);
    }

    /**
     * 保存/更新健康档案
     * PUT /api/user/profile
     * 前端传 JSON body，字段和实体一致
     */
    @PutMapping
    public ResultVO<Void> saveProfile(@AuthenticationPrincipal LoginUser loginUser,
                                      @RequestBody UserProfile profile) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        userProfileService.save(loginUser.getUserId(), profile);
        return ResultVO.ok("保存成功");
    }

}
