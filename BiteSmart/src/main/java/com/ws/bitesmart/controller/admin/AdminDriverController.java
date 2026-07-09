package com.ws.bitesmart.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.mapper.delivery.DeliveryDriverMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员端 - 配送员管理
 *
 * 管理员查看配送员列表、冻结/解冻配送员。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/drivers")
@RequiredArgsConstructor
public class AdminDriverController {

    private final DeliveryDriverMapper deliveryDriverMapper;

    /**
     * 配送员列表（分页）
     * GET /api/admin/drivers?pageNum=1&pageSize=10
     */
    @GetMapping
    public PageResultVO<DeliveryDriver> list(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<DeliveryDriver> list = deliveryDriverMapper.findAll();
        return PageResultVO.success(new PageInfo<>(list));
    }

    /**
     * 冻结/解冻配送员
     * PUT /api/admin/drivers/{id}/status?status=40
     * status: 10-在线 20-忙碌 30-离线 40-冻结
     */
    @PutMapping("/{id}/status")
    public ResultVO<Void> updateStatus(@PathVariable Long id,
                                       @RequestParam Integer status) {
        deliveryDriverMapper.updateStatus(id, status);
        log.info("管理员变更配送员状态: driverId={}, status={}", id, status);
        return ResultVO.ok(status == 40 ? "配送员已冻结" : "配送员状态已更新");
    }

}
