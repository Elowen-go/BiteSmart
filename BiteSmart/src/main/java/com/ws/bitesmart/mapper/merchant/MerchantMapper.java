package com.ws.bitesmart.mapper.merchant;

import com.ws.bitesmart.entity.merchant.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商家信息 Mapper
 */
@Mapper
public interface MerchantMapper {

    /** 根据用户ID查商家信息 */
    Merchant findByUserId(@Param("userId") Long userId);

    /** 根据商家ID查 */
    Merchant findById(@Param("id") Long id);

    /** 新增商家（入驻申请） */
    int insert(Merchant merchant);

    /** 商家修改店铺信息 */
    int updateById(Merchant merchant);

    /** 管理员审核 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("auditRemark") String auditRemark);

}
