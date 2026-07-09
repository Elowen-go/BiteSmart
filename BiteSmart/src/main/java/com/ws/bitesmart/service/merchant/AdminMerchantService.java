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
     *
     * @param merchantId   商家ID
     * @param status       审核结果：20-审核通过 30-审核驳回
     * @param auditRemark  审核备注
     * @param operatorId   操作人ID
     */
    @Transactional
    public void audit(Long merchantId, Integer status, String auditRemark, Long operatorId) {
        // 校验商家存在
        Merchant merchant = merchantMapper.findById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }

        // 更新商家状态
        merchantMapper.updateStatus(merchantId, status, auditRemark);

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

}
