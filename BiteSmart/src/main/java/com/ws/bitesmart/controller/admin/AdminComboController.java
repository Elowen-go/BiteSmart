package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.controller.merchant.MerchantComboController;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.service.dish.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/combos")
@RequiredArgsConstructor
public class AdminComboController {
    private final ComboService comboService;

    @GetMapping
    public ResultVO<?> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        var pageInfo = comboService.findAllAdmin(page, size);
        pageInfo.getList().forEach(combo -> combo.setComboImage(imageUrl(combo.getComboImage())));
        return ResultVO.success(PageResultVO.success(pageInfo));
    }

    @GetMapping("/{id}")
    public ResultVO<?> detail(@PathVariable Long id) {
        Combo combo = comboService.findById(id);
        combo.setComboImage(imageUrl(combo.getComboImage()));
        return ResultVO.success(new Detail(combo, comboService.findRelByComboId(id)));
    }

    @PostMapping
    public ResultVO<Void> add(@RequestBody MerchantComboController.ComboRequest request) {
        comboService.add(request.getCombo(), toRels(request.getDishItems()));
        return ResultVO.ok("新增成功");
    }

    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable Long id, @RequestBody MerchantComboController.ComboRequest request) {
        comboService.updateByAdmin(id, request.getCombo(), toRels(request.getDishItems()));
        return ResultVO.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) { comboService.deleteByAdmin(id); return ResultVO.ok("删除成功"); }

    private List<ComboDishRel> toRels(List<MerchantComboController.DishItem> items) {
        if (items == null) return null;
        return items.stream().map(item -> {
            ComboDishRel rel = new ComboDishRel();
            rel.setDishId(item.getDishId()); rel.setQuantity(item.getQuantity()); rel.setIsFixed(item.getIsFixed());
            return rel;
        }).toList();
    }

    public record Detail(Combo combo, List<ComboDishRel> dishItems) {}

    private String imageUrl(String value) { return value != null && value.startsWith("/uploads/") ? "/api/files/download" + value : value; }
}
