package com.ws.bitesmart.service.merchant;

import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.merchant.MerchantFundAccount;
import com.ws.bitesmart.entity.merchant.MerchantFundLedger;
import com.ws.bitesmart.entity.merchant.MerchantSettlement;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.order.PaymentLog;
import com.ws.bitesmart.entity.refund.RefundApplication;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.merchant.MerchantFundAccountMapper;
import com.ws.bitesmart.mapper.merchant.MerchantFundLedgerMapper;
import com.ws.bitesmart.mapper.merchant.MerchantSettlementMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantFinanceService {
    private static final int LEDGER_INCOME = 10;
    private static final int LEDGER_COMMISSION = 20;
    private static final int LEDGER_SETTLEMENT = 40;
    private static final int DIRECTION_CREDIT = 10;
    private static final int DIRECTION_DEBIT = 20;
    private static final int SCOPE_PENDING = 10;
    private static final int SCOPE_AVAILABLE = 20;

    private final MerchantFundAccountMapper accountMapper;
    private final MerchantFundLedgerMapper ledgerMapper;
    private final MerchantSettlementMapper settlementMapper;
    private final OrdersMapper ordersMapper;

    @Value("${platform.finance.commission-rate:0.10}")
    private BigDecimal commissionRate = BigDecimal.valueOf(0.10);

    @Transactional(rollbackFor = Exception.class)
    public void recordPaymentIncome(Orders order, PaymentLog paymentLog) {
        String incomeKey = "PAYMENT:" + order.getOrderNo() + ":INCOME";
        if (ledgerMapper.findByIdempotencyKey(incomeKey) != null) {
            return;
        }

        MerchantFundAccount account = lockOrCreateAccount(order.getMerchantId());
        BigDecimal gross = money(order.getPayAmount());
        BigDecimal commission = calculateCommission(gross);
        BigDecimal pendingBefore = money(account.getPendingBalance());
        BigDecimal pendingAfterIncome = pendingBefore.add(gross);
        insertLedger(order, paymentLog, LEDGER_INCOME, DIRECTION_CREDIT, pendingBefore,
                pendingAfterIncome, gross, incomeKey, "Order payment income", SCOPE_PENDING);

        BigDecimal pendingAfterCommission = pendingAfterIncome.subtract(commission);
        insertLedger(order, paymentLog, LEDGER_COMMISSION, DIRECTION_DEBIT, pendingAfterIncome,
                pendingAfterCommission, commission, "PAYMENT:" + order.getOrderNo() + ":COMMISSION",
                "Platform commission", SCOPE_PENDING);

        account.setPendingBalance(pendingAfterCommission);
        account.setTotalIncome(money(account.getTotalIncome()).add(gross));
        account.setTotalCommission(money(account.getTotalCommission()).add(commission));
        updateAccount(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public void releasePendingIncome(Orders order) {
        String releaseKey = "ORDER:" + order.getOrderNo() + ":RELEASE";
        if (ledgerMapper.findByIdempotencyKey(releaseKey + ":PENDING") != null) {
            return;
        }

        MerchantFundAccount account = lockOrCreateAccount(order.getMerchantId());
        BigDecimal net = money(order.getPayAmount()).subtract(calculateCommission(order.getPayAmount()));
        BigDecimal pendingBefore = money(account.getPendingBalance());
        if (pendingBefore.compareTo(net) < 0) {
            throw new BusinessException("Insufficient pending merchant balance");
        }
        BigDecimal pendingAfter = pendingBefore.subtract(net);
        BigDecimal availableBefore = money(account.getAvailableBalance());
        BigDecimal availableAfter = availableBefore.add(net);

        insertLedger(order, null, LEDGER_SETTLEMENT, DIRECTION_DEBIT, pendingBefore, pendingAfter,
                net, releaseKey + ":PENDING", "Order completed from pending balance", SCOPE_PENDING);
        insertLedger(order, null, LEDGER_SETTLEMENT, DIRECTION_CREDIT, availableBefore, availableAfter,
                net, releaseKey + ":AVAILABLE", "Order completed to available balance", SCOPE_AVAILABLE);

        account.setPendingBalance(pendingAfter);
        account.setAvailableBalance(availableAfter);
        updateAccount(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordRefund(Orders order, RefundApplication refund, PaymentLog paymentLog) {
        String refundKey = "REFUND:" + refund.getId() + ":MERCHANT";
        if (ledgerMapper.findByIdempotencyKey(refundKey) != null) {
            return;
        }

        MerchantFundAccount account = lockOrCreateAccount(order.getMerchantId());
        BigDecimal gross = money(refund.getRefundAmount());
        BigDecimal commission = calculateCommission(gross);
        BigDecimal net = gross.subtract(commission);
        boolean completed = order.getOrderStatus() != null && order.getOrderStatus() >= 50;
        BigDecimal before = completed ? money(account.getAvailableBalance()) : money(account.getPendingBalance());
        if (before.compareTo(net) < 0) {
            throw new BusinessException("Insufficient merchant balance for refund");
        }
        BigDecimal after = before.subtract(net);
        insertRefundLedger(order, refund, paymentLog, before, after, net, refundKey,
                completed ? SCOPE_AVAILABLE : SCOPE_PENDING);

        if (completed) {
            account.setAvailableBalance(after);
        } else {
            account.setPendingBalance(after);
        }
        account.setTotalRefund(money(account.getTotalRefund()).add(gross));
        account.setTotalCommission(money(account.getTotalCommission()).subtract(commission).max(BigDecimal.ZERO));
        updateAccount(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public MerchantFundAccount getOrCreateAccount(Long merchantId) {
        MerchantFundAccount account = accountMapper.findByMerchantId(merchantId);
        if (account == null) {
            account = lockOrCreateAccount(merchantId);
        }
        if (money(account.getTotalIncome()).compareTo(BigDecimal.ZERO) == 0) {
            backfillHistoricalIncome(merchantId);
            MerchantFundAccount refreshed = accountMapper.findByMerchantId(merchantId);
            if (refreshed != null) account = refreshed;
        }
        return account;
    }

    private void backfillHistoricalIncome(Long merchantId) {
        List<Orders> orders = ordersMapper.findPaidByMerchantId(merchantId);
        if (orders == null || orders.isEmpty()) return;
        for (Orders order : orders) {
            if (order.getPayAmount() == null) continue;
            String incomeKey = "PAYMENT:" + order.getOrderNo() + ":INCOME";
            if (ledgerMapper.findByIdempotencyKey(incomeKey) != null) continue;
            recordPaymentIncome(order, null);
            if (order.getOrderStatus() != null && order.getOrderStatus() == 50) {
                releasePendingIncome(order);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public MerchantSettlement createSettlement(Long merchantId, BigDecimal amount, String payoutMethod,
                                               String payoutAccountMask, Long operatorId) {
        BigDecimal netAmount = money(amount);
        if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Settlement amount must be greater than zero");
        }
        MerchantFundAccount account = lockOrCreateAccount(merchantId);
        BigDecimal availableBefore = money(account.getAvailableBalance());
        if (availableBefore.compareTo(netAmount) < 0) {
            throw new BusinessException("Settlement amount exceeds available balance");
        }

        MerchantSettlement settlement = new MerchantSettlement();
        settlement.setId(SnowflakeUtil.generate());
        settlement.setMerchantId(merchantId);
        settlement.setSettlementNo("SET" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        settlement.setGrossAmount(netAmount);
        settlement.setCommissionAmount(BigDecimal.ZERO.setScale(2));
        settlement.setRefundAmount(BigDecimal.ZERO.setScale(2));
        settlement.setNetAmount(netAmount);
        settlement.setSettlementStatus(10);
        settlement.setPayoutMethod(payoutMethod);
        settlement.setPayoutAccountMask(payoutAccountMask);
        settlement.setOperatorId(operatorId);
        settlementMapper.insert(settlement);

        BigDecimal availableAfter = availableBefore.subtract(netAmount);
        BigDecimal frozenBefore = money(account.getFrozenBalance());
        BigDecimal frozenAfter = frozenBefore.add(netAmount);
        insertSettlementLedger(settlement, 20, availableBefore, availableAfter, netAmount,
                "SETTLEMENT:" + settlement.getId() + ":FREEZE:AVAILABLE", SCOPE_AVAILABLE);
        insertSettlementLedger(settlement, 10, frozenBefore, frozenAfter, netAmount,
                "SETTLEMENT:" + settlement.getId() + ":FREEZE:FROZEN", SCOPE_AVAILABLE);
        account.setAvailableBalance(availableAfter);
        account.setFrozenBalance(frozenAfter);
        updateAccount(account);
        return settlement;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveSettlement(Long id, Long operatorId, String remark) {
        MerchantSettlement settlement = requireSettlement(id);
        if (settlement.getSettlementStatus() == 20 || settlement.getSettlementStatus() == 30) return;
        transitionSettlement(settlement, 10, 20, operatorId, remark, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeSettlement(Long id, Long operatorId, String remark) {
        MerchantSettlement settlement = requireSettlement(id);
        if (settlement.getSettlementStatus() == 30) return;
        if (settlement.getSettlementStatus() != 20) {
            throw new BusinessException("Only approved settlements can be completed");
        }
        MerchantFundAccount account = lockOrCreateAccount(settlement.getMerchantId());
        BigDecimal amount = money(settlement.getNetAmount());
        BigDecimal frozenBefore = money(account.getFrozenBalance());
        if (frozenBefore.compareTo(amount) < 0) {
            throw new BusinessException("Frozen balance is insufficient for settlement");
        }
        BigDecimal frozenAfter = frozenBefore.subtract(amount);
        insertSettlementLedger(settlement, 20, frozenBefore, frozenAfter, amount,
                "SETTLEMENT:" + id + ":COMPLETE", SCOPE_AVAILABLE);
        account.setFrozenBalance(frozenAfter);
        updateAccount(account);
        transitionSettlement(settlement, 20, 30, operatorId, remark, LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectSettlement(Long id, Long operatorId, String remark) {
        MerchantSettlement settlement = requireSettlement(id);
        if (settlement.getSettlementStatus() == 40) return;
        if (settlement.getSettlementStatus() != 10 && settlement.getSettlementStatus() != 20) {
            throw new BusinessException("Only pending or approved settlements can be rejected");
        }
        MerchantFundAccount account = lockOrCreateAccount(settlement.getMerchantId());
        BigDecimal amount = money(settlement.getNetAmount());
        BigDecimal frozenBefore = money(account.getFrozenBalance());
        if (frozenBefore.compareTo(amount) < 0) {
            throw new BusinessException("Frozen balance is insufficient for rejection");
        }
        BigDecimal frozenAfter = frozenBefore.subtract(amount);
        BigDecimal availableAfter = money(account.getAvailableBalance()).add(amount);
        insertSettlementLedger(settlement, 10, frozenBefore, frozenAfter, amount,
                "SETTLEMENT:" + id + ":REJECT:FROZEN", SCOPE_AVAILABLE);
        insertSettlementLedger(settlement, 20, money(account.getAvailableBalance()), availableAfter, amount,
                "SETTLEMENT:" + id + ":REJECT:AVAILABLE", SCOPE_AVAILABLE);
        account.setFrozenBalance(frozenAfter);
        account.setAvailableBalance(availableAfter);
        updateAccount(account);
        transitionSettlement(settlement, settlement.getSettlementStatus(), 40, operatorId, remark, null);
    }

    private MerchantSettlement requireSettlement(Long id) {
        MerchantSettlement settlement = settlementMapper.findById(id);
        if (settlement == null) throw new BusinessException("Settlement does not exist");
        return settlement;
    }

    private void transitionSettlement(MerchantSettlement settlement, int expectedStatus, int status,
                                      Long operatorId, String remark, LocalDateTime paidTime) {
        if (settlementMapper.transitionStatus(settlement.getId(), expectedStatus, status,
                operatorId, remark, paidTime) != 1) {
            throw new BusinessException("Settlement status changed, please refresh and retry");
        }
    }

    private MerchantFundAccount lockOrCreateAccount(Long merchantId) {
        MerchantFundAccount account = accountMapper.findByMerchantIdForUpdate(merchantId);
        if (account == null) {
            account = new MerchantFundAccount();
            account.setId(SnowflakeUtil.generate());
            account.setMerchantId(merchantId);
            account.setPendingBalance(BigDecimal.ZERO);
            account.setAvailableBalance(BigDecimal.ZERO);
            account.setFrozenBalance(BigDecimal.ZERO);
            account.setTotalIncome(BigDecimal.ZERO);
            account.setTotalRefund(BigDecimal.ZERO);
            account.setTotalCommission(BigDecimal.ZERO);
            account.setVersion(0L);
            accountMapper.insert(account);
        }
        return account;
    }

    private BigDecimal calculateCommission(BigDecimal amount) {
        return money(amount).multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
    }

    private void insertLedger(Orders order, PaymentLog paymentLog, int type, int direction,
                              BigDecimal before, BigDecimal after, BigDecimal amount,
                              String idempotencyKey, String remark, int balanceScope) {
        MerchantFundLedger ledger = new MerchantFundLedger();
        ledger.setId(SnowflakeUtil.generate());
        ledger.setMerchantId(order.getMerchantId());
        ledger.setOrderId(order.getId());
        ledger.setPaymentLogId(paymentLog == null ? null : paymentLog.getId());
        ledger.setLedgerType(type);
        ledger.setDirection(direction);
        ledger.setBalanceScope(balanceScope);
        ledger.setAmount(amount);
        ledger.setBalanceBefore(before);
        ledger.setBalanceAfter(after);
        ledger.setIdempotencyKey(idempotencyKey);
        ledger.setStatus(20);
        ledger.setRemark(remark);
        ledgerMapper.insert(ledger);
    }

    private void insertRefundLedger(Orders order, RefundApplication refund, PaymentLog paymentLog,
                                    BigDecimal before, BigDecimal after, BigDecimal amount,
                                    String idempotencyKey, int balanceScope) {
        MerchantFundLedger ledger = new MerchantFundLedger();
        ledger.setId(SnowflakeUtil.generate());
        ledger.setMerchantId(order.getMerchantId());
        ledger.setOrderId(order.getId());
        ledger.setPaymentLogId(paymentLog == null ? null : paymentLog.getId());
        ledger.setRefundId(refund.getId());
        ledger.setLedgerType(30);
        ledger.setDirection(DIRECTION_DEBIT);
        ledger.setBalanceScope(balanceScope);
        ledger.setAmount(amount);
        ledger.setBalanceBefore(before);
        ledger.setBalanceAfter(after);
        ledger.setIdempotencyKey(idempotencyKey);
        ledger.setStatus(20);
        ledger.setRemark("Refund reversal");
        ledgerMapper.insert(ledger);
    }

    private void insertSettlementLedger(MerchantSettlement settlement, int direction,
                                        BigDecimal before, BigDecimal after, BigDecimal amount,
                                        String idempotencyKey, int balanceScope) {
        if (ledgerMapper.findByIdempotencyKey(idempotencyKey) != null) return;
        MerchantFundLedger ledger = new MerchantFundLedger();
        ledger.setId(SnowflakeUtil.generate());
        ledger.setMerchantId(settlement.getMerchantId());
        ledger.setSettlementId(settlement.getId());
        ledger.setLedgerType(40);
        ledger.setDirection(direction);
        ledger.setBalanceScope(balanceScope);
        ledger.setAmount(amount);
        ledger.setBalanceBefore(before);
        ledger.setBalanceAfter(after);
        ledger.setIdempotencyKey(idempotencyKey);
        ledger.setStatus(20);
        ledger.setRemark("Settlement balance transition");
        ledgerMapper.insert(ledger);
    }

    private void updateAccount(MerchantFundAccount account) {
        if (accountMapper.updateBalances(account) != 1) {
            throw new BusinessException("Merchant fund account changed, please retry");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
