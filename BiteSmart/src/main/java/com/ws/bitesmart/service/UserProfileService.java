package com.ws.bitesmart.service;

import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.UserProfile;
import com.ws.bitesmart.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 健康档案服务
 *
 * 每个用户只有一份档案，首次创建，后续更新。
 * 前端传入的 JSON 字段（dietPreference 等）直接存字符串到数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;

    /**
     * 获取用户的健康档案
     */
    public UserProfile getByUserId(Long userId) {
        return userProfileMapper.findByUserId(userId);
    }

    /**
     * 保存或更新健康档案
     * 如果已有档案则更新，没有则新建
     */
    @Transactional
    public void save(Long userId, UserProfile profile) {
        UserProfile exist = userProfileMapper.findByUserId(userId);
        profile.setUserId(userId);

        if (exist != null) {
            profile.setId(exist.getId());
            userProfileMapper.updateByUserId(profile);
            log.info("健康档案已更新: userId={}", userId);
        } else {
            profile.setId(SnowflakeUtil.generate());
            userProfileMapper.insert(profile);
            log.info("健康档案已创建: userId={}", userId);
        }
    }

}
