package com.ws.bitesmart.service.user;

import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.alibaba.fastjson2.JSON;
import com.ws.bitesmart.entity.health.WeightRecord;
import com.ws.bitesmart.entity.user.UserProfile;
import com.ws.bitesmart.mapper.health.WeightRecordMapper;
import com.ws.bitesmart.mapper.user.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.time.LocalDate;

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
    private final WeightRecordMapper weightRecordMapper;

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
        profile.setDietPreference(normalizeJsonArray(profile.getDietPreference()));
        profile.setAllergyInfo(normalizeJsonArray(profile.getAllergyInfo()));
        profile.setDiseaseHistory(normalizeJsonArray(profile.getDiseaseHistory()));
        profile.setFocusParts(normalizeJsonArray(profile.getFocusParts()));

        if (exist != null) {
            profile.setId(exist.getId());
            userProfileMapper.updateByUserId(profile);
            log.info("健康档案已更新: userId={}", userId);
        } else {
            profile.setId(SnowflakeUtil.generate());
            userProfileMapper.insert(profile);
            log.info("健康档案已创建: userId={}", userId);
        }
        syncWeightRecord(userId, profile.getWeight());
    }

    private void syncWeightRecord(Long userId, java.math.BigDecimal weight) {
        if (weight == null) return;
        LocalDate today = LocalDate.now();
        WeightRecord record = weightRecordMapper.findByUserIdAndDate(userId, today);
        if (record == null) {
            record = new WeightRecord();
            record.setId(SnowflakeUtil.generate());
            record.setUserId(userId);
            record.setRecordDate(today);
            record.setWeight(weight);
            weightRecordMapper.insert(record);
        } else {
            record.setWeight(weight);
            weightRecordMapper.updateByUserIdAndDate(record);
        }
    }

    private String normalizeJsonArray(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            JSON.parseArray(value);
            return value;
        } catch (Exception ignored) {
            String normalized = Arrays.stream(value.split("[,，、;；\\s]+"))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .collect(Collectors.joining("\u0000"));
            if (normalized.isEmpty()) return null;
            return JSON.toJSONString(Arrays.asList(normalized.split("\u0000", -1)));
        }
    }

}
