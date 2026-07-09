package com.ws.bitesmart.controller;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.UserAddress;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户地址接口
 *
 * CRUD，每个用户最多可添加多个地址。
 * is_default=1 表示默认地址。
 */
@Slf4j
@RestController
@RequestMapping("/api/user/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    public ResultVO<List<UserAddress>> list(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(userAddressService.getAddresses(loginUser.getUserId()));
    }

    @PostMapping
    public ResultVO<Void> add(@AuthenticationPrincipal LoginUser loginUser,
                              @RequestBody UserAddress address) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        userAddressService.add(loginUser.getUserId(), address);
        return ResultVO.ok("添加成功");
    }

    @PutMapping("/{id}")
    public ResultVO<Void> update(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable Long id,
                                 @RequestBody UserAddress address) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        address.setId(id);
        userAddressService.update(loginUser.getUserId(), address);
        return ResultVO.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        userAddressService.delete(id, loginUser.getUserId());
        return ResultVO.ok("删除成功");
    }

}
