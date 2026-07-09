package com.ws.bitesmart.mapper.merchant;

import com.ws.bitesmart.entity.merchant.MerchantAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商家入驻审核日志 Mapper
 */
@Mapper
public interface MerchantAuditLogMapper {

    /** 查某个商家的审核记录 */
    List<MerchantAuditLog> findByMerchantId(@Param("merchantId") Long merchantId);

    /** 新增审核记录 */
    int insert(MerchantAuditLog log);

}
