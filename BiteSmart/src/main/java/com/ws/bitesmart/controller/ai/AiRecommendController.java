package com.ws.bitesmart.controller.ai;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.ai.AiRecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI食谱推荐接口
 *
 * 基于用户的健康档案和营养标准，生成个性化饮食推荐。
 * 用户需要先填写健康档案才能获取推荐。
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRecommendController {

    private final AiRecommendService aiRecommendService;

    /**
     * 获取 AI 食谱推荐
     * GET /api/ai/recommend
     *
     * 如果用户没填健康档案，返回提示信息。
     */
    @GetMapping("/recommend")
    public ResultVO<Map<String, Object>> recommend(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Map<String, Object> result = aiRecommendService.recommend(loginUser.getUserId());
        return ResultVO.success(result);
    }

}
