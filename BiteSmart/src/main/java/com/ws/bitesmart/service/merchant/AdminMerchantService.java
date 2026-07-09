package com.ws.bitesmart.service.merchant;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.entity.merchant.MerchantAuditLog;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.merchant.MerchantAuditLogMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员端 - 商家审核服务
 *
 * 处理商家入驻审核业务，更新商家状态并记录审核日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMerchantService {

    private final MerchantMapper merchantMapper;
    private final MerchantAuditLogMapper auditLogMapper;

    /**
     * 分页查询商家列表
     */
    public PageInfo<Merchant> findAll(Integer status, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Merchant> list = merchantMapper.findAll(status);
        return new PageInfo<>(list);
    }

    /**
     * 审核商家入驻
     * 仅待审核(10)状态的商家可以审核
     *
     * @param merchantId   商家ID
     * @param status       审核结果：20-审核通过 30-审核驳回
     * @param auditRemark  审核备注
     * @param operatorId   操作人ID
     */
    @Transactional
    public void audit(Long merchantId, Integer status, String auditRemark, Long operatorId) {
        // 校验商家存在且为待审核状态
        Merchant merchant = merchantMapper.findById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        if (merchant.getStatus() != 10) {
            throw new BusinessException("当前商家状态不允许审核，仅待审核状态的商家可操作");
        }

        // 更新商家状态（带乐观锁：防止重复审核）
        int affected = merchantMapper.updateStatus(merchantId, status, auditRemark);
        if (affected == 0) {
            throw new BusinessException("审核失败，商家状态已变更");
        }

        // 记录审核日志
        MerchantAuditLog auditLog = new MerchantAuditLog();
        auditLog.setId(SnowflakeUtil.generate());
        auditLog.setMerchantId(merchantId);
        auditLog.setSubmitTime(LocalDateTime.now());
        auditLog.setAuditOperatorId(operatorId);
        auditLog.setAuditStatus(status);
        auditLog.setAuditRemark(auditRemark);
        auditLog.setAuditTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);

        log.info("商家审核: merchantId={}, status={}, remark={}, operatorId={}",
                merchantId, status, auditRemark, operatorId);
    }

    /**
     * 关闭商家店铺（审核通过→已关闭）
     * 仅审核通过(20)状态的商家可以关闭
     */
    @Transactional
    public void close(Long merchantId, String reason, Long operatorId) {
        Merchant merchant = merchantMapper.findById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        if (merchant.getStatus() != 20) {
            throw new BusinessException("仅已审核通过的商家可以执行关店操作");
        }

        int affected = merchantMapper.updateStatus(merchantId, 40, reason);
        if (affected == 0) {
            throw new BusinessException("关店失败，商家状态已变更");
        }

        // 记录审核日志
        MerchantAuditLog auditLog = new MerchantAuditLog();
        auditLog.setId(SnowflakeUtil.generate());
        auditLog.setMerchantId(merchantId);
        auditLog.setSubmitTime(LocalDateTime.now());
        auditLog.setAuditOperatorId(operatorId);
        auditLog.setAuditStatus(40);
        auditLog.setAuditRemark(reason);
        auditLog.setAuditTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);

        log.info("商家已关店: merchantId={}, reason={}, operatorId={}", merchantId, reason, operatorId);
    }

}
