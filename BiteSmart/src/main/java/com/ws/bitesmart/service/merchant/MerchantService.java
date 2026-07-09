package com.ws.bitesmart.service.merchant;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.entity.merchant.MerchantAuditLog;
import com.ws.bitesmart.entity.user.SysUser;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.merchant.MerchantAuditLogMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 商家服务
 *
 * 入驻流程：商家提交资料 → 状态=待审核 → 管理员审核 → 通过/驳回
 * 入驻后可以维护店铺信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantMapper merchantMapper;
    private final MerchantAuditLogMapper auditLogMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 商家入驻申请
     * 校验用户是否存在且角色是否为商家，创建商家记录
     */
    @Transactional
    public void apply(Long userId, Merchant merchant) {
        // 查用户信息
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USERNAME_NOT_FOUND);
        }

        // 检查是否已经申请过
        Merchant exist = merchantMapper.findByUserId(userId);
        if (exist != null) {
            throw new BusinessException("你已经提交过入驻申请，请等待审核");
        }

        // 创建商家记录
        merchant.setId(userId); // merchant.id = user.id
        merchant.setUserId(userId);
        merchant.setStatus(10); // 待审核
        merchantMapper.insert(merchant);

        // 记录审核日志
        MerchantAuditLog auditLog = new MerchantAuditLog();
        auditLog.setId(SnowflakeUtil.generate());
        auditLog.setMerchantId(userId);
        auditLog.setSubmitTime(LocalDateTime.now());
        auditLog.setAuditStatus(10); // 待审核
        auditLogMapper.insert(auditLog);

        log.info("商家入驻申请: userId={}, shopName={}", userId, merchant.getShopName());
    }

    /**
     * 获取商家信息
     */
    public Merchant getByUserId(Long userId) {
        Merchant merchant = merchantMapper.findByUserId(userId);
        if (merchant == null) {
            throw new BusinessException("你还未提交入驻申请");
        }
        return merchant;
    }

    /**
     * 更新店铺信息
     */
    @Transactional
    public void updateShopInfo(Long userId, Merchant merchant) {
        Merchant exist = getByUserId(userId);
        merchant.setId(exist.getId());
        merchant.setUserId(userId);
        merchantMapper.updateById(merchant);
        log.info("店铺信息更新: userId={}", userId);
    }

    /**
     * 获取商家审核记录
     */
    public MerchantAuditLog getAuditLog(Long userId) {
        Merchant exist = getByUserId(userId);
        var logs = auditLogMapper.findByMerchantId(exist.getId());
        return logs.isEmpty() ? null : logs.get(0);
    }

}
