package com.ws.bitesmart.service.order;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.order.ShoppingCart;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.ComboMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.order.ShoppingCartMapper;
import com.ws.bitesmart.service.delivery.DeliveryTaskService;
import com.ws.bitesmart.service.system.OperateLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 订单服务
 *
 * 核心业务：创建订单（从购物车选中商品生成订单）、订单状态流转、查询。
 * 订单状态：10-待支付 20-待接单 30-备餐中 40-配送中 50-已完成 60-已取消
 *
 * 高并发安全：
 * - 库存扣减使用乐观锁（WHERE stock >= #{quantity}），返回0表示库存不足
 * - 订单创建和库存扣减在同一事务中
 * - 取消/拒单时释放锁定库存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShoppingCartMapper shoppingCartMapper;
    private final DishMapper dishMapper;
    private final ComboMapper comboMapper;
    private final ComboDishRelMapper comboDishRelMapper;
    private final OperateLogService operateLogService;
    private final DeliveryTaskService deliveryTaskService;

    /** 订单号序列计数器（确保同一毫秒内不重复） */
    private static final AtomicLong ORDER_NO_SEQ = new AtomicLong(0);

    /**
     * 生成订单号
     * yyyyMMddHHmmss + 3位序列号 + 4位随机数
     * 序列号保证同一毫秒内唯一，随机数防止被遍历
     */
    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long seq = ORDER_NO_SEQ.incrementAndGet() % 1000;
        int randomPart = (int) (Math.random() * 9000) + 1000;
        return timePart + String.format("%03d", seq) + randomPart;
    }

    /**
     * 获取商品的商家ID
     */
    private Long getMerchantId(Integer itemType, Long dishId, Long comboId) {
        if (itemType == 10) {
            Dish dish = dishMapper.findById(dishId);
            if (dish == null) throw new BusinessException(ResultCodeEnum.NOT_FOUND, "菜品不存在");
            return dish.getMerchantId();
        } else if (itemType == 20) {
            Combo combo = comboMapper.findById(comboId);
            if (combo == null) throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
            return combo.getMerchantId();
        }
        throw new BusinessException("商品类型错误");
    }

    /**
     * 创建订单
     *
     * 流程：查购物车选中商品 → 锁定库存（乐观锁）→ 构建订单快照 → 插入订单+明细 → 清购物车
     * 任何一步失败都会回滚，库存不会多扣。
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Long userId, String address, String receiverName,
                              String receiverPhone, String remark) {
        // 1. 查购物车中选中的商品
        List<ShoppingCart> selectedItems = shoppingCartMapper.findSelectedByUserId(userId);
        if (selectedItems == null || selectedItems.isEmpty()) {
            throw new BusinessException("请先选择要购买的商品");
        }

        // 2. 确定商家ID
        ShoppingCart firstItem = selectedItems.get(0);
        Long merchantId = getMerchantId(firstItem.getItemType(), firstItem.getDishId(), firstItem.getComboId());

        // 3. 锁定库存 + 构建订单明细
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (ShoppingCart cart : selectedItems) {
            BigDecimal price;
            String name;
            String image;
            Integer calories = 0;
            BigDecimal protein = BigDecimal.ZERO;
            BigDecimal fat = BigDecimal.ZERO;
            BigDecimal carbs = BigDecimal.ZERO;

            if (cart.getItemType() == 10) {
                // 菜品：锁定库存（乐观锁，stock>=quantity才扣减）
                Dish dish = dishMapper.findById(cart.getDishId());
                if (dish == null) throw new BusinessException("菜品已下架或不存在");

                int locked = dishMapper.lockStock(cart.getDishId(), cart.getQuantity());
                if (locked == 0) {
                    throw new BusinessException("菜品【" + dish.getDishName() + "】库存不足");
                }

                price = dish.getPrice();
                name = dish.getDishName();
                image = dish.getDishImage();
                calories = dish.getCalories();
                protein = dish.getProtein() != null ? dish.getProtein() : BigDecimal.ZERO;
                fat = dish.getFat() != null ? dish.getFat() : BigDecimal.ZERO;
                carbs = dish.getCarbs() != null ? dish.getCarbs() : BigDecimal.ZERO;
            } else if (cart.getItemType() == 20) {
                // 套餐：锁定套餐内每个菜品的库存
                Combo combo = comboMapper.findById(cart.getComboId());
                if (combo == null) throw new BusinessException("套餐已下架或不存在");

                List<ComboDishRel> rels = comboDishRelMapper.findByComboId(cart.getComboId());
                for (ComboDishRel rel : rels) {
                    int locked = dishMapper.lockStock(rel.getDishId(), rel.getQuantity() * cart.getQuantity());
                    if (locked == 0) {
                        throw new BusinessException("套餐包含的菜品库存不足，请重新选择");
                    }
                }

                price = combo.getPrice();
                name = combo.getComboName();
                image = combo.getComboImage();
                calories = combo.getTotalCalories();
                protein = combo.getTotalProtein() != null ? combo.getTotalProtein() : BigDecimal.ZERO;
                fat = combo.getTotalFat() != null ? combo.getTotalFat() : BigDecimal.ZERO;
                carbs = combo.getTotalCarbs() != null ? combo.getTotalCarbs() : BigDecimal.ZERO;
            } else {
                throw new BusinessException("商品类型错误");
            }

            BigDecimal subTotal = price.multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(subTotal);

            OrderItem item = new OrderItem();
            item.setId(SnowflakeUtil.generate());
            item.setItemType(cart.getItemType());
            item.setDishId(cart.getDishId());
            item.setComboId(cart.getComboId());
            item.setSnapshotName(name);
            item.setSnapshotImage(image);
            item.setSnapshotPrice(price);
            item.setSnapshotCalories(calories);
            item.setSnapshotProtein(protein);
            item.setSnapshotFat(fat);
            item.setSnapshotCarbs(carbs);
            item.setQuantity(cart.getQuantity());
            item.setSubTotal(subTotal);
            orderItems.add(item);
        }

        // 4. 生成订单号
        String orderNo = generateOrderNo();

        // 5. 插入订单
        Orders order = new Orders();
        order.setId(SnowflakeUtil.generate());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount);
        order.setOrderStatus(10); // 待支付
        order.setDeliveryStatus(0);
        order.setDeliveryAddress(address);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setRemark(remark);
        ordersMapper.insert(order);

        // 6. 插入明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.insertBatch(orderItems);

        // 7. 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        operateLogService.record(userId, null, null,
                "创建订单", "OrderService.createOrder", null, orderNo, null, null, null);

        log.info("订单创建成功: orderNo={}, userId={}, merchantId={}, amount={}",
                orderNo, userId, merchantId, totalAmount);
        return orderNo;
    }

    /**
     * 取消订单
     * 只在"待支付"(10)或"待接单"(20)可取消
     * 取消后释放锁定库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, Long userId, String reason) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 10 && order.getOrderStatus() != 20) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许取消");
        }

        // 释放锁定库存
        releaseLockedStock(id);

        // 乐观锁更新：仅当当前状态仍为待支付(10)或待接单(20)时才更新为已取消(60)
        int affected = ordersMapper.updateStatusWithLock(
                id, order.getOrderStatus(), 60,
                null, null, null,
                LocalDateTime.now(), reason,
                null, null);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "订单状态已变更，取消失败");
        }
        operateLogService.record(userId, null, null,
                "取消订单", "OrderService.cancelOrder", null, order.getOrderNo(), null, null, null);
        log.info("订单已取消: orderNo={}, userId={}, reason={}", order.getOrderNo(), userId, reason);
    }

    /**
     * 商家拒单
     * 释放锁定库存 + 取消订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long id, Long merchantId, String reason) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 20) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许拒单");
        }

        // 释放锁定库存
        releaseLockedStock(id);

        // 乐观锁更新：仅当当前状态为待接单(20)时才更新为已取消(60)
        int affected = ordersMapper.updateStatusWithLock(
                id, 20, 60,
                null, null, null,
                LocalDateTime.now(), reason,
                null, null);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "订单状态已变更，拒单失败");
        }
        operateLogService.record(merchantId, null, null,
                "商家拒单", "OrderService.rejectOrder", null, order.getOrderNo(), null, null, null);
        log.info("商家拒单: orderNo={}, merchantId={}, reason={}", order.getOrderNo(), merchantId, reason);
    }

    /**
     * 释放订单锁定的库存
     * 读取订单明细，对菜品直接释放，对套餐查关联菜品后逐个释放
     */
    private void releaseLockedStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            if (item.getItemType() == 10 && item.getDishId() != null) {
                // 菜品：直接释放
                dishMapper.unlockStock(item.getDishId(), item.getQuantity());
                log.debug("释放锁定库存: dishId={}, quantity={}", item.getDishId(), item.getQuantity());
            } else if (item.getItemType() == 20 && item.getComboId() != null) {
                // 套餐：释放关联菜品的库存
                List<ComboDishRel> rels = comboDishRelMapper.findByComboId(item.getComboId());
                for (ComboDishRel rel : rels) {
                    int qty = rel.getQuantity() * item.getQuantity();
                    dishMapper.unlockStock(rel.getDishId(), qty);
                    log.debug("释放套餐锁定库存: dishId={}, quantity={}", rel.getDishId(), qty);
                }
            }
        }
    }

    /** 商家接单 */
    @Transactional(rollbackFor = Exception.class)
    public void acceptOrder(Long id, Long merchantId) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 20) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许接单");
        }
        // 乐观锁更新：仅当当前状态为待接单(20)时才更新为备餐中(30)
        int affected = ordersMapper.updateStatusWithLock(
                id, 20, 30,
                null, null, null,
                null, null, null, null);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "订单状态已变更，接单失败");
        }
        operateLogService.record(merchantId, null, null,
                "商家接单", "OrderService.acceptOrder", null, order.getOrderNo(), null, null, null);
        log.info("商家已接单: orderNo={}, merchantId={}", order.getOrderNo(), merchantId);
    }

    /** 开始配送（备餐完成 → 配送中） */
    @Transactional(rollbackFor = Exception.class)
    public void startDelivering(Long id, Long merchantId) {
        finishPreparing(id, merchantId);
    }

    /** 开始备餐（仅记录日志，不改变状态。接单时状态已变为备餐中） */
    @Transactional(rollbackFor = Exception.class)
    public void prepareOrder(Long id, Long merchantId) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 30) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许开始备餐");
        }
        operateLogService.record(merchantId, null, null,
                "商家开始备餐", "OrderService.prepareOrder", null, order.getOrderNo(), null, null, null);
        log.info("商家开始备餐: orderNo={}, merchantId={}", order.getOrderNo(), merchantId);
    }

    /** 备餐完成 → 配送中 */
    @Transactional(rollbackFor = Exception.class)
    public void finishPreparing(Long id, Long merchantId) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 30) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许操作");
        }
        // 乐观锁更新：仅当当前状态为备餐中(30)时才更新为配送中(40)，同时设置配送状态为待取餐
        int affected = ordersMapper.updateStatusWithLock(
                id, 30, 40,
                null, null, null,
                null, null, null, 10);
        if (affected == 0) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "订单状态已变更，操作失败");
        }
        // 自动创建配送任务
        deliveryTaskService.createTask(order);
        log.info("出餐完成并创建配送任务: orderNo={}, merchantId={}", order.getOrderNo(), merchantId);
    }

    /** 用户查自己的订单（含明细） */
    public List<Orders> getOrdersByUser(Long userId) {
        List<Orders> orders = ordersMapper.findByUserId(userId);
        batchLoadOrderItems(orders);
        return orders;
    }

    /** 用户查自己的订单（分页） */
    public PageInfo<Orders> getOrdersByUser(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Orders> list = ordersMapper.findByUserId(userId);
        return new PageInfo<>(list);
    }

    /** 商家查收到的订单（含明细） */
    public List<Orders> getOrdersByMerchant(Long merchantId) {
        List<Orders> orders = ordersMapper.findByMerchantId(merchantId);
        batchLoadOrderItems(orders);
        return orders;
    }

    /** 商家查收到的订单（分页） */
    public PageInfo<Orders> getOrdersByMerchant(Long merchantId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Orders> list = ordersMapper.findByMerchantId(merchantId);
        return new PageInfo<>(list);
    }

    /** 用户查订单详情 */
    public Orders getOrderDetail(Long id, Long userId) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 商家查订单详情 */
    public Orders getOrderDetailForMerchant(Long id, Long merchantId) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 查订单明细 */
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.findByOrderId(orderId);
    }

    public Orders findById(Long id) {
        return ordersMapper.findById(id);
    }

    /**
     * 批量加载订单明细（防 N+1 查询）
     * 一次性查出所有订单的明细，按 orderId 分组挂到每个订单上
     */
    private void batchLoadOrderItems(List<Orders> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemMapper.findByOrderIds(orderIds);
        Map<Long, List<OrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Orders order : orders) {
            order.setItems(itemMap.getOrDefault(order.getId(), new ArrayList<>()));
        }
    }
}
