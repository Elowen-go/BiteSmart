package com.ws.bitesmart.service.dish;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.ComboMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 套餐服务
 *
 * 商家端：创建套餐（含关联菜品）、更新套餐（先删旧关联再插新关联）、上下架。
 * 套餐类型：10-减脂 20-增肌 30-控糖 40-会员专属
 * 按 merchantId 进行数据隔离。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboMapper comboMapper;
    private final ComboDishRelMapper comboDishRelMapper;

    /** 查某商家的全部套餐 */
    public List<Combo> findByMerchantId(Long merchantId) {
        return comboMapper.findByMerchantId(merchantId);
    }

    /** 查套餐详情 */
    public Combo findById(Long id) {
        Combo combo = comboMapper.findById(id);
        if (combo == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        return combo;
    }

    /** 查询上架套餐 */
    public List<Combo> findAvailable() {
        return comboMapper.findAvailable();
    }

    /**
     * 新增套餐
     *
     * @param combo   套餐信息
     * @param dishIds 关联的菜品 ID 列表
     */
    @Transactional
    public void add(Combo combo, List<Long> dishIds) {
        combo.setId(SnowflakeUtil.generate());
        if (combo.getStatus() == null) combo.setStatus(10); // 默认上架
        comboMapper.insert(combo);

        // 批量插入关联
        if (dishIds != null && !dishIds.isEmpty()) {
            List<ComboDishRel> relList = new ArrayList<>();
            for (Long dishId : dishIds) {
                ComboDishRel rel = new ComboDishRel();
                rel.setId(SnowflakeUtil.generate());
                rel.setComboId(combo.getId());
                rel.setDishId(dishId);
                rel.setQuantity(1); // 默认 1 份
                rel.setIsFixed(1);  // 默认固定不可替换
                relList.add(rel);
            }
            comboDishRelMapper.insertBatch(relList);
        }
    }

    /**
     * 修改套餐
     * 先删旧关联，再插新关联
     *
     * @param merchantId 商家 ID
     * @param id         套餐 ID
     * @param combo      套餐信息
     * @param dishIds    新的关联菜品 ID 列表
     */
    @Transactional
    public void update(Long merchantId, Long id, Combo combo, List<Long> dishIds) {
        Combo exist = comboMapper.findById(id);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        combo.setId(id);
        combo.setMerchantId(merchantId);
        comboMapper.updateById(combo);

        // 先删旧关联
        comboDishRelMapper.deleteByComboId(id);

        // 再插新关联
        if (dishIds != null && !dishIds.isEmpty()) {
            List<ComboDishRel> relList = new ArrayList<>();
            for (Long dishId : dishIds) {
                ComboDishRel rel = new ComboDishRel();
                rel.setId(SnowflakeUtil.generate());
                rel.setComboId(id);
                rel.setDishId(dishId);
                rel.setQuantity(1);
                rel.setIsFixed(1);
                relList.add(rel);
            }
            comboDishRelMapper.insertBatch(relList);
        }
    }

    /** 下架套餐，校验所属权 */
    @Transactional
    public void delete(Long merchantId, Long id) {
        Combo exist = comboMapper.findById(id);
        if (exist == null || !exist.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
        }
        Combo update = new Combo();
        update.setId(id);
        update.setStatus(20); // 下架
        comboMapper.updateById(update);
    }

    /** 查套餐包含的菜品关联列表 */
    public List<ComboDishRel> findRelByComboId(Long comboId) {
        return comboDishRelMapper.findByComboId(comboId);
    }

}
