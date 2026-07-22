package com.ws.bitesmart.controller.delivery;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.entity.delivery.DeliveryTask;
import com.ws.bitesmart.mapper.delivery.DeliveryDriverMapper;
import com.ws.bitesmart.service.delivery.DeliveryTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端 - 配送轨迹查询接口
 *
 * 用户无需登录即可查看订单的配送状态和配送员当前位置。
 */
@Slf4j
@RestController
@RequestMapping("/api/delivery/tracking")
@RequiredArgsConstructor
public class DeliveryTrackingController {

    private final DeliveryTaskService deliveryTaskService;
    private final DeliveryDriverMapper deliveryDriverMapper;

    /**
     * 查订单配送状态和配送员当前位置
     *
     * @param orderId 订单ID
     */
    @GetMapping("/{orderId}")
    public ResultVO<Map<String, Object>> tracking(@PathVariable Long orderId) {
        DeliveryTask task = deliveryTaskService.getByOrderId(orderId);
        if (task == null) {
            return ResultVO.error(404, "配送任务不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskStatus", task.getTaskStatus());
        result.put("pickupCode", task.getPickupCode());
        result.put("estimatedDeliveryTime", task.getEstimatedDeliveryTime());
        result.put("pickupTime", task.getPickupTime());
        result.put("deliverTime", task.getDeliverTime());
        result.put("currentLat", task.getCurrentLat());
        result.put("currentLng", task.getCurrentLng());
        result.put("orderRemark", task.getOrderRemark());

        // 骑手实时位置与轨迹（GCJ-02 坐标系，高德/腾讯地图直接用）
        result.put("riderLat", task.getCurrentLat());
        result.put("riderLng", task.getCurrentLng());
        result.put("locTime", task.getLocationUpdateTime());
        List<Map<String, Object>> path = deliveryTaskService.getTaskLocations(task.getId()).stream()
                .map(p -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("latitude", p.getLatitude());
                    point.put("longitude", p.getLongitude());
                    point.put("time", p.getCreatedTime());
                    return point;
                })
                .toList();
        result.put("path", path);

        // 如果有配送员，返回配送员位置
        if (task.getDriverId() != null) {
            DeliveryDriver driver = deliveryDriverMapper.findById(task.getDriverId());
            if (driver != null) {
                Map<String, Object> driverInfo = new HashMap<>();
                driverInfo.put("realName", driver.getRealName());
                driverInfo.put("phone", driver.getPhone());
                driverInfo.put("currentLat", driver.getCurrentLat());
                driverInfo.put("currentLng", driver.getCurrentLng());
                driverInfo.put("vehicleType", driver.getVehicleType());
                result.put("driver", driverInfo);
            }
        }

        return ResultVO.success(result);
    }
}
