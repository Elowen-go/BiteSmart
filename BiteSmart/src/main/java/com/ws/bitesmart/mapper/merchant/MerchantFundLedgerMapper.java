package com.ws.bitesmart.mapper.merchant;

import com.ws.bitesmart.entity.merchant.MerchantFundLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MerchantFundLedgerMapper {
    MerchantFundLedger findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
    int insert(MerchantFundLedger ledger);
    List<MerchantFundLedger> findByMerchantId(@Param("merchantId") Long merchantId);
    List<MerchantFundLedger> findAll(@Param("merchantId") Long merchantId,
                                     @Param("ledgerType") Integer ledgerType);
}
