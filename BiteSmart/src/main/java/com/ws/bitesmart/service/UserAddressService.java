package com.ws.bitesmart.service;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.UserAddress;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
