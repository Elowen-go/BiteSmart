package com.ws.bitesmart.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.user.SysUser;
import com.ws.bitesmart.mapper.complaint.ComplaintTicketMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import com.ws.bitesmart.mapper.user.UserMembershipMapper;
import com.ws.bitesmart.mapper.user.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final int RECENT_RECORD_LIMIT = 5;

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final OrdersMapper ordersMapper;
    private final ComplaintTicketMapper complaintTicketMapper;

    @GetMapping
    public PageResultVO<SysUser> list(@RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<SysUser> list = sysUserMapper.findAll();
        return PageResultVO.success(new PageInfo<>(list));
    }

    @GetMapping("/{id}")
    public ResultVO<Map<String, Object>> detail(@PathVariable Long id) {
        SysUser user = sysUserMapper.findById(id);
        if (user == null) {
            return ResultVO.error(404, "用户不存在");
        }

        user.setPassword(null);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("user", user);
        detail.put("profile", userProfileMapper.findByUserId(id));
        detail.put("membership", userMembershipMapper.findActiveByUserId(id));
        detail.put("orders", limitRecent(ordersMapper.findByUserId(id)));
        detail.put("complaints", limitRecent(complaintTicketMapper.findByUserId(id)));
        return ResultVO.success(detail);
    }

    private static <T> List<T> limitRecent(List<T> records) {
        if (records == null || records.size() <= RECENT_RECORD_LIMIT) {
            return records == null ? List.of() : records;
        }
        return records.subList(0, RECENT_RECORD_LIMIT);
    }

    @PutMapping("/{id}/status")
    public ResultVO<Void> updateStatus(@PathVariable Long id,
                                       @RequestParam Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        sysUserMapper.updateById(user);
        log.info("Admin changed user status: userId={}, status={}", id, status);
        return ResultVO.ok(status == 10 ? "用户已启用" : "用户已冻结");
    }
}
