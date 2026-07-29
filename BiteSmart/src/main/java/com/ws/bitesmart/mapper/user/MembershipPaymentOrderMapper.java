package com.ws.bitesmart.mapper.user;

import com.ws.bitesmart.entity.user.MembershipPaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MembershipPaymentOrderMapper {

    MembershipPaymentOrder findByOrderNo(@Param("orderNo") String orderNo);

    int insert(MembershipPaymentOrder order);

    int markPaidIfPending(@Param("orderNo") String orderNo,
                          @Param("transactionNo") String transactionNo);
}
