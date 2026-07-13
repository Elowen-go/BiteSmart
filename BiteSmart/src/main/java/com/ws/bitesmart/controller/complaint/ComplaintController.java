package com.ws.bitesmart.controller.complaint;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.complaint.ComplaintTicket;
import com.ws.bitesmart.mapper.complaint.ComplaintTicketMapper;
import com.ws.bitesmart.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {
    private final ComplaintTicketMapper complaintMapper;

    @PostMapping
    public ResultVO<Void> create(@AuthenticationPrincipal LoginUser loginUser,
                                 @RequestBody ComplaintTicket ticket) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (ticket.getOrderId() == null || ticket.getTargetType() == null ||
                ticket.getTargetId() == null || ticket.getComplaintReason() == null ||
                ticket.getComplaintReason().isBlank()) {
            return ResultVO.error(400, "投诉信息不完整");
        }
        ticket.setId(SnowflakeUtil.generate());
        ticket.setUserId(loginUser.getUserId());
        ticket.setStatus(10);
        complaintMapper.insert(ticket);
        return ResultVO.ok("投诉已提交");
    }
}
