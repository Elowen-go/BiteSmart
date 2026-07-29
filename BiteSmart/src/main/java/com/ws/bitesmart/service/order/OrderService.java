package com.ws.bitesmart.service.order;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.dto.order.ComboCustomizationSnapshot;
import com.ws.bitesmart.entity.dish.Combo;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.order.ShoppingCart;
import com.ws.bitesmart.entity.refund.RefundApplication;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.ComboMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.order.ShoppingCartMapper;
import com.ws.bitesmart.mapper.refund.RefundApplicationMapper;
import com.ws.bitesmart.service.delivery.DeliveryTaskService;
import com.ws.bitesmart.service.dish.ComboService;
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
import java.util.LinkedHashMap;
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
    private final ComboService comboService;
    private final OperateLogService operateLogService;
    private final DeliveryTaskService deliveryTaskService;
    private final MerchantMapper merchantMapper;
    private final RefundApplicationMapper refundApplicationMapper;

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
        return createOrder(userId, address, receiverName, receiverPhone, remark, null, null);
    }

    /** 创建订单并保存收货地址坐标，供配送任务导航使用。 */
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Long userId, String address, String receiverName,
                              String receiverPhone, String remark,
                              BigDecimal deliveryLat, BigDecimal deliveryLng) {
        List<ShoppingCart> selectedItems = shoppingCartMapper.findSelectedByUserId(userId);
        if (selectedItems == null || selectedItems.isEmpty()) {
            throw new BusinessException("请先选择要购买的商品");
        }
        Map<Long, List<ShoppingCart>> grouped = groupByMerchant(selectedItems);
        if (grouped.size() != 1) {
            throw new BusinessException("购物车中包含不同商家的商品，请使用分商家结算");
        }
        Map.Entry<Long, List<ShoppingCart>> group = grouped.entrySet().iterator().next();
        return createOrderForItems(userId, address, receiverName, receiverPhone, remark,
                deliveryLat, deliveryLng, group.getKey(), group.getValue());
    }

    /** 一次结算按商家拆成多个订单，整个过程保持在同一事务中。 */
    @Transactional(rollbackFor = Exception.class)
    public List<String> createOrders(Long userId, String address, String receiverName,
                                     String receiverPhone, Map<Long, String> remarksByMerchant) {
        List<ShoppingCart> selectedItems = shoppingCartMapper.findSelectedByUserId(userId);
        if (selectedItems == null || selectedItems.isEmpty()) {
            throw new BusinessException("请先选择要购买的商品");
        }
        Map<Long, List<ShoppingCart>> grouped = groupByMerchant(selectedItems);
        List<String> orderNos = new ArrayList<>();
        for (Map.Entry<Long, List<ShoppingCart>> entry : grouped.entrySet()) {
            String remark = remarksByMerchant == null ? null : remarksByMerchant.get(entry.getKey());
            orderNos.add(createOrderForItems(userId, address, receiverName, receiverPhone,
                    remark, null, null, entry.getKey(), entry.getValue()));
        }
        shoppingCartMapper.deleteByUserId(userId);
        return orderNos;
    }

    private Map<Long, List<ShoppingCart>> groupByMerchant(List<ShoppingCart> selectedItems) {
        Map<Long, List<ShoppingCart>> grouped = new LinkedHashMap<>();
        for (ShoppingCart cart : selectedItems) {
            if (cart.getQuantity() == null || cart.getQuantity() < 1 || cart.getQuantity() > 99) {
                throw new BusinessException("商品数量必须在1到99之间");
            }
            Long merchantId = getMerchantId(cart.getItemType(), cart.getDishId(), cart.getComboId());
            grouped.computeIfAbsent(merchantId, key -> new ArrayList<>()).add(cart);
        }
        return grouped;
    }

    private String createOrderForItems(Long userId, String address, String receiverName,
                                       String receiverPhone, String remark,
                                       BigDecimal deliveryLat, BigDecimal deliveryLng,
                                       Long merchantId,
                                       List<ShoppingCart> selectedItems) {
        // 店铺打烊则拒绝下单（open_status=20）
        Merchant merchant = merchantMapper.findById(merchantId);
        if (merchant != null && Integer.valueOf(Merchant.OPEN_STATUS_CLOSED).equals(merchant.getOpenStatus())) {
            throw new BusinessException("店铺【" + merchant.getShopName() + "】已打烊，暂不接受下单");
        }

        // 锁定库存 + 构建订单明细
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
            String snapshotNutritionJson = null;

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

                ComboCustomizationSnapshot snapshot = buildOrderComboSnapshot(cart);
                for (ComboCustomizationSnapshot.SelectedDishItem rel : snapshot.getItems()) {
                    int locked = dishMapper.lockStock(rel.getDishId(), rel.getQuantity() * cart.getQuantity());
                    if (locked == 0) {
                        throw new BusinessException("套餐包含的菜品库存不足，请重新选择");
                    }
                }

                price = combo.getPrice();
                name = combo.getComboName();
                image = combo.getComboImage();
                calories = snapshot.getTotalCalories();
                protein = snapshot.getTotalProtein() != null ? snapshot.getTotalProtein() : BigDecimal.ZERO;
                fat = snapshot.getTotalFat() != null ? snapshot.getTotalFat() : BigDecimal.ZERO;
                carbs = snapshot.getTotalCarbs() != null ? snapshot.getTotalCarbs() : BigDecimal.ZERO;
                snapshotNutritionJson = JSON.toJSONString(snapshot);
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
            item.setSnapshotNutritionJson(snapshotNutritionJson);
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
        order.setChannel("PC");
        order.setLockStockTime(LocalDateTime.now());
        order.setAutoCancelTime(LocalDateTime.now().plusMinutes(30));
        order.setDeliveryAddress(address);
        order.setDeliveryLat(deliveryLat);
        order.setDeliveryLng(deliveryLng);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setRemark(remark);
        ordersMapper.insert(order);

        // 6. 插入明细
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.insertBatch(orderItems);

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
     * 用户申请退款：待接单、备餐中、配送中的订单可以提交退款工单。
     * 退款由后台审核，用户提交后订单仍保留原业务状态，避免审核驳回时丢失原状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long id, Long userId, String reason, String desc) {
        Orders order = ordersMapper.findById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() == null ||
                (order.getOrderStatus() != 20 && order.getOrderStatus() != 30 &&
                        order.getOrderStatus() != 40)) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不支持退款申请");
        }

        RefundApplication latest = refundApplicationMapper.findLatestByOrderId(id);
        if (latest != null && latest.getAuditStatus() != null &&
                (latest.getAuditStatus() == 10 || latest.getAuditStatus() == 20 || latest.getAuditStatus() == 40)) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "该订单已有退款申请，请等待处理结果");
        }

        RefundApplication application = new RefundApplication();
        application.setId(SnowflakeUtil.generate());
        application.setOrderId(order.getId());
        application.setOrderNo(order.getOrderNo());
        application.setUserId(userId);
        application.setRefundAmount(order.getPayAmount() != null ? order.getPayAmount() : order.getTotalAmount());
        application.setRefundReason(reason == null || reason.trim().isEmpty() ? "其他原因" : reason.trim());
        application.setRefundDesc(desc == null || desc.trim().isEmpty() ? null : desc.trim());
        application.setAuditStatus(10);
        application.setApplyTime(LocalDateTime.now());
        refundApplicationMapper.insert(application);

        operateLogService.record(userId, null, null,
                "提交退款申请", "OrderService.applyRefund", null, order.getOrderNo(), null, null, null);
        log.info("用户已提交退款申请: orderNo={}, userId={}", order.getOrderNo(), userId);
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
    private ComboCustomizationSnapshot buildOrderComboSnapshot(ShoppingCart cart) {
        List<ComboCustomizationSnapshot.ReplacementItem> replacements = List.of();
        if (cart.getCustomizationJson() != null && !cart.getCustomizationJson().isEmpty()) {
            ComboCustomizationSnapshot customization = JSON.parseObject(cart.getCustomizationJson(), ComboCustomizationSnapshot.class);
            if (customization != null && customization.getReplacements() != null) {
                replacements = customization.getReplacements();
            }
        }
        return comboService.buildCustomizedSnapshot(cart.getComboId(), replacements);
    }

    private List<ComboCustomizationSnapshot.SelectedDishItem> resolveSnapshotItems(OrderItem item) {
        if (item.getSnapshotNutritionJson() != null && !item.getSnapshotNutritionJson().isEmpty()) {
            ComboCustomizationSnapshot snapshot = JSON.parseObject(item.getSnapshotNutritionJson(), ComboCustomizationSnapshot.class);
            if (snapshot != null && snapshot.getItems() != null && !snapshot.getItems().isEmpty()) {
                return snapshot.getItems();
            }
        }
        List<ComboCustomizationSnapshot.SelectedDishItem> items = new ArrayList<>();
        List<ComboDishRel> rels = comboDishRelMapper.findByComboId(item.getComboId());
        for (ComboDishRel rel : rels) {
            ComboCustomizationSnapshot.SelectedDishItem selected = new ComboCustomizationSnapshot.SelectedDishItem();
            selected.setDishId(rel.getDishId());
            selected.setQuantity(rel.getQuantity() == null ? 1 : rel.getQuantity());
            items.add(selected);
        }
        return items;
    }

    private void releaseLockedStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            if (item.getItemType() == 10 && item.getDishId() != null) {
                // 菜品：直接释放
                dishMapper.unlockStock(item.getDishId(), item.getQuantity());
                log.debug("释放锁定库存: dishId={}, quantity={}", item.getDishId(), item.getQuantity());
            } else if (item.getItemType() == 20 && item.getComboId() != null) {
                // 套餐：释放关联菜品的库存
                for (ComboCustomizationSnapshot.SelectedDishItem rel : resolveSnapshotItems(item)) {
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
        batchLoadOrderItems(list);
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
