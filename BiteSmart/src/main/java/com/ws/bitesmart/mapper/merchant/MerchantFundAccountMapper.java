package com.ws.bitesmart.mapper.merchant;

import com.ws.bitesmart.entity.merchant.MerchantFundAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MerchantFundAccountMapper {
    MerchantFundAccount findByMerchantId(@Param("merchantId") Long merchantId);
    MerchantFundAccount findByMerchantIdForUpdate(@Param("merchantId") Long merchantId);
    List<MerchantFundAccount> findAll();
    int insert(MerchantFundAccount account);
    int updateBalances(MerchantFundAccount account);
}
