package com.ws.bitesmart.service.user;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.dto.response.LoginResponseDTO;
import com.ws.bitesmart.entity.user.SysUser;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务
 *
 * 处理用户信息查询、更新等操作。
 * 目前主要负责：
 * - 根据用户ID查询用户信息（给 Controller 和 Admin 管理用）
 * - 后续会加：密码修改、账号冻结/解冻等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息（不含密码），注意要转成安全的DTO返回
     */
    public SysUser getUserById(Long userId) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USERNAME_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 把 SysUser 转成安全的 UserInfo（去掉密码等敏感字段）
     */
    public LoginResponseDTO.UserInfo toUserInfo(SysUser user) {
        return LoginResponseDTO.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roleType(user.getRoleType())
                .build();
    }

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param updateData 更新数据（仅更新 nickname, avatar, phone, email）
     * @return 更新后的用户信息
     */
    public SysUser updateUser(Long userId, SysUser updateData) {
        SysUser user = getUserById(userId);
        if (updateData.getNickname() != null) {
            user.setNickname(updateData.getNickname());
        }
        if (updateData.getAvatar() != null) {
            user.setAvatar(updateData.getAvatar());
        }
        if (updateData.getPhone() != null) {
            user.setPhone(updateData.getPhone());
        }
        if (updateData.getEmail() != null) {
            user.setEmail(updateData.getEmail());
        }
        user.setUpdateTime(java.time.LocalDateTime.now());
        sysUserMapper.updateById(user);
        return user;
    }

}
