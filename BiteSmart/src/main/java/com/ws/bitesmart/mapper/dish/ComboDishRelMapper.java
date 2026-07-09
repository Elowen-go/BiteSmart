package com.ws.bitesmart.mapper.dish;

import com.ws.bitesmart.entity.dish.ComboDishRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 套餐菜品关联 Mapper
 *
 * 维护套餐与菜品的多对多关系，支持先删后加模式。
 */
@Mapper
public interface ComboDishRelMapper {

    /** 查套餐包含的所有菜品关联记录 */
    List<ComboDishRel> findByComboId(@Param("comboId") Long comboId);

    /** 批量插入套餐菜品关联 */
    int insertBatch(@Param("list") List<ComboDishRel> relList);

    /** 删除套餐的所有菜品关联（软删除） */
    int deleteByComboId(@Param("comboId") Long comboId);

}
