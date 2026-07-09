package com.ws.bitesmart.mapper.dish;

import com.ws.bitesmart.entity.dish.Combo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 套餐 Mapper
 *
 * 套餐是一组菜品的组合，通过 ComboDishRel 与菜品关联。
 */
@Mapper
public interface ComboMapper {

    /** 查某商家的全部套餐，按创建时间倒序 */
    List<Combo> findByMerchantId(@Param("merchantId") Long merchantId);

    /** 根据 ID 查询套餐 */
    Combo findById(@Param("id") Long id);

    /** 查询上架套餐（status=10） */
    List<Combo> findAvailable();

    /** 新增套餐 */
    int insert(Combo combo);

    /** 修改套餐（动态 SQL，只改非空字段） */
    int updateById(Combo combo);

    /** 更新套餐销量 */
    int updateSalesCount(@Param("id") Long id,
                         @Param("salesCount") Integer salesCount);

}
