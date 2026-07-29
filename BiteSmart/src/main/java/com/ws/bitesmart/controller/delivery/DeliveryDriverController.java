package com.ws.bitesmart.controller.delivery;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.dto.request.RiderLocationRequest;
import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.entity.delivery.DeliveryTask;
import com.ws.bitesmart.entity.delivery.DriverSettlement;
import com.ws.bitesmart.entity.review.Review;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.delivery.DeliveryDriverService;
import com.ws.bitesmart.service.delivery.DeliveryTaskService;
import com.ws.bitesmart.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 配送员端 - 配送接口
 *
 * 配送员注册、状态管理、位置上传、接单/取餐/送达等操作。
 */
@Slf4j
@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DeliveryDriverController {

    private final DeliveryDriverService deliveryDriverService;
    private final DeliveryTaskService deliveryTaskService;
    private final ReviewService reviewService;

    /**
     * 注册为配送员
     */
    @PostMapping("/register")
    public ResultVO<Void> register(@AuthenticationPrincipal LoginUser loginUser,
                                    @RequestParam String realName,
                                    @RequestParam String phone,
                                    @RequestParam String idCard,
                                    @RequestParam Integer vehicleType) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        deliveryDriverService.register(loginUser.getUserId(), realName, phone, idCard, vehicleType);
        return ResultVO.ok("注册成功");
    }

    /**
     * 更新在线状态
     *
     * @param status 10-在线 20-忙碌 30-离线
     */
    @PostMapping("/status")
    public ResultVO<Void> updateStatus(@AuthenticationPrincipal LoginUser loginUser,
                                        @RequestParam Integer status) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        deliveryDriverService.updateStatus(loginUser.getUserId(), status);
        return ResultVO.ok("状态更新成功");
    }

    /**
     * 上传配送轨迹点
     *
     * body: {"taskId": 任务ID, "latitude": 纬度, "longitude": 经度}
     * 坐标系约定：GCJ-02（高德/腾讯地图坐标系），
     * 小程序端用 wx.getLocation({ type: 'gcj02' }) 获取或自行转换后上传。
     * 仅允许任务属于当前骑手且处于配送途中（30-已取餐 / 40-配送中）时上传。
     */
    @PostMapping("/location")
    public ResultVO<Void> updateLocation(@AuthenticationPrincipal LoginUser loginUser,
                                          @RequestBody RiderLocationRequest request) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (request.getTaskId() == null || request.getLatitude() == null || request.getLongitude() == null) {
            return ResultVO.error(400, "taskId、latitude、longitude 不能为空");
        }
        deliveryTaskService.uploadTaskLocation(loginUser.getUserId(), request.getTaskId(),
                request.getLatitude(), request.getLongitude());
        return ResultVO.ok("位置更新成功");
    }

    // ==================== 个人信息 ====================

    /**
     * 获取当前骑手个人信息
     */
    @GetMapping("/profile")
    public ResultVO<DeliveryDriver> getProfile(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(deliveryDriverService.getProfile(loginUser.getUserId()));
    }

    /**
     * 更新骑手个人信息
     * body 字段：realName / phone / vehicleType(10-电动车 20-自行车 30-汽车) / serviceArea(JSON字符串)
     * 只传要改的字段即可
     */
    @PutMapping("/profile")
    public ResultVO<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                         @RequestBody DeliveryDriver request) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        deliveryDriverService.updateProfile(loginUser.getUserId(), request);
        return ResultVO.ok("资料更新成功");
    }

    /**
     * 我的配送任务列表
     */
    @GetMapping("/tasks")
    public ResultVO<List<DeliveryTask>> tasks(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(deliveryTaskService.getDriverTasks(loginUser.getUserId()));
    }

    /**
     * 任务大厅：待接单任务列表（task_status=10，所有配送员可见，抢单制）
     */
    @GetMapping("/tasks/pending")
    public ResultVO<List<DeliveryTask>> pendingTasks(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(deliveryTaskService.getPendingTasks());
    }

    /**
     * 接单
     */
    @PostMapping("/tasks/{id}/accept")
    public ResultVO<Void> acceptTask(@AuthenticationPrincipal LoginUser loginUser,
                                      @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        deliveryTaskService.acceptTask(id, loginUser.getUserId());
        return ResultVO.ok("接单成功");
    }

    /**
     * 已取餐
     */
    @PostMapping("/tasks/{id}/pickup")
    public ResultVO<Void> pickupTask(@AuthenticationPrincipal LoginUser loginUser,
                                      @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        deliveryTaskService.pickupTask(id, loginUser.getUserId());
        return ResultVO.ok("取餐成功");
    }

    /**
     * 开始配送：30 已取餐 -> 40 配送中。
     */
    @PostMapping("/tasks/{id}/start")
    public ResultVO<Void> startDelivery(@AuthenticationPrincipal LoginUser loginUser,
                                         @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        deliveryTaskService.startDeliveryTask(id, loginUser.getUserId());
        return ResultVO.ok("已开始配送");
    }

    /**
     * 骑手拒单：任务回待接单池（taskStatus 20→10），记录拒单原因
     *
     * @param id     配送任务ID
     * @param reason 拒单原因（可选）
     */
    @PostMapping("/tasks/{id}/reject")
    public ResultVO<Void> rejectTask(@AuthenticationPrincipal LoginUser loginUser,
                                      @PathVariable Long id,
                                      @RequestParam(required = false) String reason) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        deliveryTaskService.rejectTask(loginUser.getUserId(), id, reason);
        return ResultVO.ok("已拒单，任务返回待接单池");
    }

    /**
     * 已送达
     */
    @PostMapping("/tasks/{id}/deliver")
    public ResultVO<Void> deliverTask(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        deliveryTaskService.deliverTask(id, loginUser.getUserId());
        return ResultVO.ok("送达成功");
    }

    // ==================== 收入统计 ====================

    /**
     * 结算记录列表
     */
    @GetMapping("/settlements")
    public ResultVO<List<DriverSettlement>> settlements(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(deliveryDriverService.getSettlements(loginUser.getUserId()));
    }

    /**
     * 收入统计汇总
     */
    @GetMapping("/settlements/stats")
    public ResultVO<Map<String, Object>> settlementStats(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(deliveryDriverService.getSettlementStats(loginUser.getUserId()));
    }

    // ==================== 异常上报 ====================

    /**
     * 上报配送异常
     *
     * @param id     配送任务ID
     * @param reason 异常原因
     */
    @PostMapping("/tasks/{id}/exception")
    public ResultVO<Void> reportException(@AuthenticationPrincipal LoginUser loginUser,
                                           @PathVariable Long id,
                                           @RequestParam String reason) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        deliveryTaskService.reportException(loginUser.getUserId(), id, reason);
        return ResultVO.ok("异常上报成功");
    }

    // ==================== 评价查看 ====================

    /**
     * 我收到的评价列表
     */
    @GetMapping("/reviews")
    public ResultVO<List<Review>> reviews(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(reviewService.findByDriverId(loginUser.getUserId()));
    }

    /**
     * 配送评分统计
     */
    @GetMapping("/reviews/stats")
    public ResultVO<Map<String, Object>> reviewStats(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(reviewService.getDriverRatingStats(loginUser.getUserId()));
    }
}
