package com.ws.bitesmart.mapper.merchant;

import com.ws.bitesmart.entity.merchant.MerchantSettlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MerchantSettlementMapper {
    MerchantSettlement findById(@Param("id") Long id);
    List<MerchantSettlement> findByMerchantId(@Param("merchantId") Long merchantId);
    List<MerchantSettlement> findAll(@Param("merchantId") Long merchantId,
                                     @Param("status") Integer status);
    int insert(MerchantSettlement settlement);
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("operatorId") Long operatorId,
                     @Param("operatorRemark") String operatorRemark,
                     @Param("paidTime") java.time.LocalDateTime paidTime);
    int transitionStatus(@Param("id") Long id,
                         @Param("expectedStatus") Integer expectedStatus,
                         @Param("status") Integer status,
                         @Param("operatorId") Long operatorId,
                         @Param("operatorRemark") String operatorRemark,
                         @Param("paidTime") java.time.LocalDateTime paidTime);
}
