package com.ws.bitesmart.service.user;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.user.UserAddress;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.user.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户地址服务
 *
 * 每个用户可添加多个地址，通过 is_default 标记默认地址。
 * 增删改都会校验地址的所属权，防止越权操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressMapper userAddressMapper;

    public List<UserAddress> getAddresses(Long userId) {
        return userAddressMapper.findByUserId(userId);
    }

    public UserAddress getById(Long id, Long userId) {
        UserAddress address = userAddressMapper.findById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "地址不存在");
        }
        return address;
    }

    @Transactional
    public void add(Long userId, UserAddress address) {
        address.setId(SnowflakeUtil.generate());
        address.setUserId(userId);
        if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
        // 如果设为默认，先清除其他默认地址
        if (address.getIsDefault() == 1) {
            userAddressMapper.resetDefault(userId);
        }
        userAddressMapper.insert(address);
        log.info("新增地址: userId={}, id={}", userId, address.getId());
    }

    @Transactional
    public void update(Long userId, UserAddress address) {
        getById(address.getId(), userId); // 校验所属权
        address.setUserId(userId);
        if (address.getIsDefault() == 1) {
            userAddressMapper.resetDefault(userId);
        }
        userAddressMapper.updateById(address);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        getById(id, userId); // 校验所属权
        userAddressMapper.deleteById(id);
    }

}
