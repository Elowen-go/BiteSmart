package com.ws.bitesmart.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.complaint.ComplaintTicket;
import com.ws.bitesmart.entity.refund.RefundApplication;
import com.ws.bitesmart.mapper.complaint.ComplaintTicketMapper;
import com.ws.bitesmart.mapper.refund.RefundApplicationMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/admin/work-orders")
@RequiredArgsConstructor
public class AdminWorkOrderController {
    private final RefundApplicationMapper refundMapper;
    private final ComplaintTicketMapper complaintMapper;
    private final OrdersMapper ordersMapper;
    private final SysUserMapper sysUserMapper;
    private final MerchantMapper merchantMapper;

    @GetMapping("/refunds")
    public PageResultVO<RefundApplication> refunds(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<RefundApplication> list = refundMapper.findAll(status);
        return PageResultVO.success(new PageInfo<>(list));
    }

    @GetMapping("/refunds/{id}")
    public ResultVO<Map<String, Object>> refundDetail(@PathVariable Long id) {
        RefundApplication item = refundMapper.findById(id);
        if (item == null) return ResultVO.error(404, "退款工单不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workOrder", item);
        result.put("order", ordersMapper.findById(item.getOrderId()));
        result.put("user", sysUserMapper.findById(item.getUserId()));
        result.put("merchant", result.get("order") == null ? null : merchantMapper.findById(((com.ws.bitesmart.entity.order.Orders) result.get("order")).getMerchantId()));
        return ResultVO.success(result);
    }

    @PutMapping("/refunds/{id}")
    public ResultVO<Void> auditRefund(@PathVariable Long id,
                                      @RequestParam Integer status,
                                      @RequestParam(required = false) String remark) {
        if (status != 20 && status != 30 && status != 40) {
            return ResultVO.error(400, "退款处理状态不合法");
        }
        int affected = refundMapper.updateAudit(id, status, null, remark);
        return affected > 0 ? ResultVO.ok("退款工单已处理") : ResultVO.error(409, "退款工单不存在或已处理");
    }

    @GetMapping("/complaints")
    public PageResultVO<ComplaintTicket> complaints(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<ComplaintTicket> list = complaintMapper.findAll(status);
        return PageResultVO.success(new PageInfo<>(list));
    }

    @GetMapping("/complaints/{id}")
    public ResultVO<Map<String, Object>> complaintDetail(@PathVariable Long id) {
        ComplaintTicket item = complaintMapper.findById(id);
        if (item == null) return ResultVO.error(404, "投诉工单不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workOrder", item);
        result.put("order", ordersMapper.findById(item.getOrderId()));
        result.put("user", sysUserMapper.findById(item.getUserId()));
        result.put("merchant", result.get("order") == null ? null : merchantMapper.findById(((com.ws.bitesmart.entity.order.Orders) result.get("order")).getMerchantId()));
        return ResultVO.success(result);
    }

    @PutMapping("/complaints/{id}")
    public ResultVO<Void> handleComplaint(@PathVariable Long id,
                                           @RequestParam Integer status,
                                           @RequestParam(required = false) String remark,
                                           @RequestParam(required = false) String result) {
        if (status != 20 && status != 30 && status != 40) {
            return ResultVO.error(400, "投诉处理状态不合法");
        }
        int affected = complaintMapper.updateHandle(id, status, null, remark, result);
        return affected > 0 ? ResultVO.ok("投诉工单已处理") : ResultVO.error(409, "投诉工单不存在");
    }
}
