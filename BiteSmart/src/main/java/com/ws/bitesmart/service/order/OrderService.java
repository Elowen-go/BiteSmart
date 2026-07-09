package com.ws.bitesmart.service.order;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.order.ShoppingCart;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.ComboMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.order.ShoppingCartMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 订单服务
 *
 * 核心业务：创建订单（从购物车选中商品生成订单）、订单状态流转、查询。
 * 订单状态：10-待支付 20-待接单 30-备餐中 40-配送中 50-已完成 60-已取消
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

    /**
     * 生成订单号：yyyyMMddHHmmss + 6位随机数
     */
    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = new Random().nextInt(900000) + 100000;
        return timePart + randomPart;
    }

    /**
     * 获取商品的商家ID
     * 根据 itemType 分别查询菜品或套餐
     */
    private Long getMerchantId(Integer itemType, Long dishId, Long comboId) {
        if (itemType == 10) {
            // 菜品
            Dish dish = dishMapper.findById(dishId);
            if (dish == null) {
                throw new BusinessException(ResultCodeEnum.NOT_FOUND, "菜品不存在");
            }
            return dish.getMerchantId();
        } else if (itemType == 20) {
            // 套餐
            Combo combo = comboMapper.findById(comboId);
            if (combo == null) {
                throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐不存在");
            }
            return combo.getMerchantId();
        }
        throw new BusinessException("商品类型错误");
    }

    /**
     * 创建订单
     *
     * @param userId        用户ID
     * @param address       配送地址
     * @param receiverName  收货人姓名
     * @param receiverPhone 收货人电话
     * @param remark        订单备注
     * @return 订单号
     */
    @Transactional
    public String createOrder(Long userId, String address, String receiverName,
                              String receiverPhone, String remark) {
        // 1. 查购物车中选中的商品
        List<ShoppingCart> selectedItems = shoppingCartMapper.findSelectedByUserId(userId);
        if (selectedItems == null || selectedItems.isEmpty()) {
            throw new BusinessException("请先选择要购买的商品");
        }

        // 2. 确定商家ID（取第一个商品的商家）
        ShoppingCart firstItem = selectedItems.get(0);
        Long merchantId = getMerchantId(firstItem.getItemType(), firstItem.getDishId(), firstItem.getComboId());

        // 3. 计算总价并构建订单明细快照
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
                // 菜品
                Dish dish = dishMapper.findById(cart.getDishId());
                if (dish == null) {
                    throw new BusinessException(ResultCodeEnum.NOT_FOUND, "菜品已下架或不存在");
                }
                price = dish.getPrice();
                name = dish.getDishName();
                image = dish.getDishImage();
                calories = dish.getCalories();
                protein = dish.getProtein() != null ? dish.getProtein() : BigDecimal.ZERO;
                fat = dish.getFat() != null ? dish.getFat() : BigDecimal.ZERO;
                carbs = dish.getCarbs() != null ? dish.getCarbs() : BigDecimal.ZERO;
            } else if (cart.getItemType() == 20) {
                // 套餐
                Combo combo = comboMapper.findById(cart.getComboId());
                if (combo == null) {
                    throw new BusinessException(ResultCodeEnum.NOT_FOUND, "套餐已下架或不存在");
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

            // 构建订单明细快照
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

        // 5. 插入 orders 表
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

        // 6. 插入 order_item 明细（补上 orderId）
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.insertBatch(orderItems);

        // 7. 清空购物车中已选中的商品
        shoppingCartMapper.deleteByUserId(userId);

        log.info("订单创建成功: orderNo={}, userId={}, merchantId={}, amount={}",
                orderNo, userId, merchantId, totalAmount);
        return orderNo;
    }

    /**
     * 取消订单
     * 只能在"待支付"(10)或"待接单"(20)状态下取消
     */
    @Transactional
    public void cancelOrder(Long id, Long userId, String reason) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 10 && order.getOrderStatus() != 20) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许取消");
        }
        Orders update = new Orders();
        update.setId(id);
        update.setOrderStatus(60); // 已取消
        update.setCancelTime(LocalDateTime.now());
        update.setCancelReason(reason);
        ordersMapper.updateStatus(update);
        log.info("订单已取消: orderNo={}, userId={}, reason={}", order.getOrderNo(), userId, reason);
    }

    /**
     * 商家接单
     * 从"待支付"(20) → "备餐中"(30)
     * 注：待支付状态支付后自动变为待接单
     */
    @Transactional
    public void acceptOrder(Long id, Long merchantId) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 20) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许接单");
        }
        Orders update = new Orders();
        update.setId(id);
        update.setOrderStatus(30); // 备餐中
        ordersMapper.updateStatus(update);
        log.info("商家已接单: orderNo={}, merchantId={}", order.getOrderNo(), merchantId);
    }

    /**
     * 商家拒单
     * 只能在"待接单"(20)状态下拒单
     */
    @Transactional
    public void rejectOrder(Long id, Long merchantId, String reason) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 20) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许拒单");
        }
        Orders update = new Orders();
        update.setId(id);
        update.setOrderStatus(60); // 已取消
        update.setCancelTime(LocalDateTime.now());
        update.setCancelReason(reason);
        ordersMapper.updateStatus(update);
        log.info("商家拒单: orderNo={}, merchantId={}, reason={}", order.getOrderNo(), merchantId, reason);
    }

    /**
     * 备餐中
     * "备餐中"(30) → "配送中"(40)
     */
    @Transactional
    public void startDelivering(Long id, Long merchantId) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 30) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许操作");
        }
        Orders update = new Orders();
        update.setId(id);
        update.setOrderStatus(40); // 配送中
        ordersMapper.updateStatus(update);
        log.info("订单开始配送: orderNo={}, merchantId={}", order.getOrderNo(), merchantId);
    }

    /**
     * 出餐完成
     * "备餐中"(30) → "配送中"(40)
     * 这是简化版本，没有配送模块时出餐完成直接置为配送中
     */
    @Transactional
    public void finishPreparing(Long id, Long merchantId) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 30) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许操作");
        }
        Orders update = new Orders();
        update.setId(id);
        update.setOrderStatus(40); // 配送中
        ordersMapper.updateStatus(update);
        log.info("出餐完成，已转为配送中: orderNo={}, merchantId={}", order.getOrderNo(), merchantId);
    }

    /** 用户查自己的订单 */
    public List<Orders> getOrdersByUser(Long userId) {
        return ordersMapper.findByUserId(userId);
    }

    /** 商家查收到的订单 */
    public List<Orders> getOrdersByMerchant(Long merchantId) {
        return ordersMapper.findByMerchantId(merchantId);
    }

    /** 查订单详情（含明细） */
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

    /** 根据ID查订单（内部使用） */
    public Orders findById(Long id) {
        return ordersMapper.findById(id);
    }
}
