package com.ws.bitesmart.controller.plan;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.dto.request.PlanCheckRequest;
import com.ws.bitesmart.dto.request.PlanGenerateRequest;
import com.ws.bitesmart.dto.request.PlanSwapRequest;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.plan.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 专属计划接口
 *
 * 用户完成健康档案（问卷）后生成 7 天三餐的专属食谱计划，
 * 支持开始执行、当天打卡（同步饮食记录）、换菜、演示推进天数。
 * 返回结构统一为 { plan, meals }，meals 联表带出菜品信息。
 */
@Slf4j
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /**
     * 生成专属计划（基于当前用户健康档案）
     * POST /api/plan/generate
     * body 可选：{"planDays": 7 或 21}，21 天需生效会员，否则自动降级为 7
     */
    @PostMapping("/generate")
    public ResultVO<Map<String, Object>> generate(@AuthenticationPrincipal LoginUser loginUser,
                                                   @RequestBody(required = false) PlanGenerateRequest request) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Integer planDays = request == null ? null : request.getPlanDays();
        return ResultVO.success("计划生成成功", planService.generate(loginUser.getUserId(), planDays));
    }

    /**
     * 查询当前用户最新计划（含全部餐次）
     * GET /api/plan/current
     */
    @GetMapping("/current")
    public ResultVO<Map<String, Object>> current(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(planService.current(loginUser.getUserId()));
    }

    /**
     * 开始执行计划
     * POST /api/plan/start
     */
    @PostMapping("/start")
    public ResultVO<Map<String, Object>> start(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success("计划已开始", planService.start(loginUser.getUserId()));
    }

    /**
     * 打卡 / 取消打卡（只允许当天餐次；打卡同步写 diet_record）
     * POST /api/plan/check
     */
    @PostMapping("/check")
    public ResultVO<Map<String, Object>> check(@AuthenticationPrincipal LoginUser loginUser,
                                                @RequestBody PlanCheckRequest request) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (request.getMealId() == null || request.getChecked() == null) {
            return ResultVO.error(400, "mealId 和 checked 不能为空");
        }
        Map<String, Object> detail = planService.check(loginUser.getUserId(), request.getMealId(), request.getChecked());
        return ResultVO.success(request.getChecked() ? "已打卡，同步到饮食记录" : "已取消打卡", detail);
    }

    /**
     * 换一道菜（按菜品池偏移重新选菜）
     * POST /api/plan/swap
     */
    @PostMapping("/swap")
    public ResultVO<Map<String, Object>> swap(@AuthenticationPrincipal LoginUser loginUser,
                                               @RequestBody PlanSwapRequest request) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (request.getMealId() == null) {
            return ResultVO.error(400, "mealId 不能为空");
        }
        return ResultVO.success("已换一道菜", planService.swap(loginUser.getUserId(), request.getMealId()));
    }

    /**
     * 演示用：进入下一天（最后一天自动完成计划）
     * POST /api/plan/advance
     */
    @PostMapping("/advance")
    public ResultVO<Map<String, Object>> advance(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success("已进入下一天", planService.advance(loginUser.getUserId()));
    }

}
