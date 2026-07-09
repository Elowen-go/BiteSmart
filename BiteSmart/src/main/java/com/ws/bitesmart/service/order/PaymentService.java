package com.ws.bitesmart.service.order;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.ComboDishRel;
import com.ws.bitesmart.entity.order.OrderItem;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.order.PaymentLog;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.ComboDishRelMapper;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.order.OrderItemMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.order.PaymentLogMapper;
import com.ws.bitesmart.service.system.OperateLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 支付服务
 *
 * 目前为模拟实现，直接置为支付成功。
 * 后续可接入支付宝沙箱或微信支付。
 * payMethod：10-支付宝 20-微信
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrdersMapper ordersMapper;
    private final PaymentLogMapper paymentLogMapper;
    private final DishMapper dishMapper;
    private final OrderItemMapper orderItemMapper;
    private final ComboDishRelMapper comboDishRelMapper;
    private final OperateLogService operateLogService;

    /**
     * 模拟支付
     *
     * @param orderNo   订单编号
     * @param payMethod 支付方式：10-支付宝 20-微信
     */
    @Transactional
    public void pay(String orderNo, Integer payMethod) {
        // 1. 查订单
        Orders order = ordersMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() != 10) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "当前订单状态不允许支付");
        }

        // 2. 生成模拟交易流水号
        String transactionNo = "MOCK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        // 3. 更新订单状态为"待接单"(20)，记录支付信息
        Orders update = new Orders();
        update.setId(order.getId());
        update.setOrderStatus(20); // 待接单
        update.setPayMethod(payMethod);
        update.setPayAmount(order.getPayAmount());
        update.setPayTime(LocalDateTime.now());
        ordersMapper.updateStatus(update);

        // 4. 插入支付流水
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setId(SnowflakeUtil.generate());
        paymentLog.setOrderId(order.getId());
        paymentLog.setOrderNo(orderNo);
        paymentLog.setPayMethod(payMethod);
        paymentLog.setTransactionNo(transactionNo);
        paymentLog.setPayAmount(order.getPayAmount());
        paymentLog.setPayStatus(20); // 支付成功
        paymentLog.setPayTime(LocalDateTime.now());
        paymentLogMapper.insert(paymentLog);

        // 5. 支付成功后扣减实际库存（从 lock_stock 中扣除）
        List<OrderItem> items = orderItemMapper.findByOrderId(order.getId());
        for (OrderItem item : items) {
            if (item.getItemType() == 10 && item.getDishId() != null) {
                dishMapper.deductLockedStock(item.getDishId(), item.getQuantity());
            } else if (item.getItemType() == 20 && item.getComboId() != null) {
                List<ComboDishRel> rels = comboDishRelMapper.findByComboId(item.getComboId());
                for (ComboDishRel rel : rels) {
                    dishMapper.deductLockedStock(rel.getDishId(), rel.getQuantity() * item.getQuantity());
                }
            }
        }

        operateLogService.record(order.getUserId(), null, null,
                "订单支付", "PaymentService.pay", null, orderNo, null, null, null);

        log.info("模拟支付成功: orderNo={}, payMethod={}, transactionNo={}, amount={}",
                orderNo, payMethod, transactionNo, order.getPayAmount());
    }
}
