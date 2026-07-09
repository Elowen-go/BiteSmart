package com.ws.bitesmart.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.user.SysUser;
import com.ws.bitesmart.mapper.user.SysUserMapper;
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
 * 管理员端 - 用户管理
 *
 * 管理员管理所有系统用户，包括查看列表/详情、启用/禁用用户。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserMapper sysUserMapper;

    /**
     * 用户列表（分页）
     * GET /api/admin/users?pageNum=1&pageSize=10
     */
    @GetMapping
    public PageResultVO<SysUser> list(@RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<SysUser> list = sysUserMapper.findAll();
        return PageResultVO.success(new PageInfo<>(list));
    }

    /**
     * 用户详情
     * GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResultVO<SysUser> detail(@PathVariable Long id) {
        SysUser user = sysUserMapper.findById(id);
        if (user == null) {
            return ResultVO.error(404, "用户不存在");
        }
        // 不返回密码
        user.setPassword(null);
        return ResultVO.success(user);
    }

    /**
     * 启用/禁用用户
     * PUT /api/admin/users/{id}/status?status=10
     * status: 10-正常 20-冻结
     */
    @PutMapping("/{id}/status")
    public ResultVO<Void> updateStatus(@PathVariable Long id,
                                       @RequestParam Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        sysUserMapper.updateById(user);
        log.info("管理员变更用户状态: userId={}, status={}", id, status);
        return ResultVO.ok(status == 10 ? "用户已启用" : "用户已冻结");
    }

}
