package com.ws.bitesmart.service.delivery;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.dto.request.DriverFeedbackRequest;
import com.ws.bitesmart.entity.complaint.ComplaintTicket;
import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.entity.delivery.DeliveryTask;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.complaint.ComplaintTicketMapper;
import com.ws.bitesmart.mapper.delivery.DeliveryDriverMapper;
import com.ws.bitesmart.mapper.delivery.DeliveryTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** 骑手端反馈服务，和用户投诉接口保持独立。 */
@Service
@RequiredArgsConstructor
public class DriverFeedbackService {

    private static final int PLATFORM_TARGET_TYPE = 30;
    private static final Set<String> CATEGORIES = Set.of("配送任务", "商家协作", "结算收入", "平台服务", "其他建议");

    private final DeliveryDriverMapper deliveryDriverMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final ComplaintTicketMapper complaintTicketMapper;

    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, DriverFeedbackRequest request) {
        if (request == null || request.getOrderId() == null ||
                request.getCategory() == null || request.getCategory().isBlank() ||
                request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "反馈信息不完整");
        }
        if (!CATEGORIES.contains(request.getCategory())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "反馈分类无效");
        }
        if (request.getContent().trim().length() > 300) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "反馈内容不能超过300字");
        }

        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        DeliveryTask task = deliveryTaskMapper.findByOrderId(request.getOrderId());
        if (task == null || task.getDriverId() == null || !driver.getId().equals(task.getDriverId())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "该订单不属于当前配送员");
        }

        ComplaintTicket ticket = new ComplaintTicket();
        ticket.setId(SnowflakeUtil.generate());
        ticket.setOrderId(task.getOrderId());
        ticket.setUserId(userId);
        ticket.setTargetType(PLATFORM_TARGET_TYPE);
        ticket.setTargetId(driver.getId());
        ticket.setComplaintReason("骑手反馈·" + request.getCategory());
        ticket.setComplaintDesc(request.getContent().trim());
        ticket.setStatus(10);
        complaintTicketMapper.insert(ticket);
    }
}
