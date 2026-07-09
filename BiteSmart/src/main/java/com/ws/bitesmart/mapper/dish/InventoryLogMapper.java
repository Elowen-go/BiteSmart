package com.ws.bitesmart.mapper.dish;

import com.ws.bitesmart.entity.dish.InventoryLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 库存变动流水 Mapper
 *
 * 支持按商家维度查询库存变动记录。
 */
@Mapper
public interface InventoryLogMapper {

    /** 查某商家的库存变动记录，按时间倒序 */
    List<InventoryLog> findByMerchantId(@Param("merchantId") Long merchantId);

}
