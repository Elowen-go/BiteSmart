package com.ws.bitesmart.service.merchant;

import com.ws.bitesmart.entity.merchant.MerchantFundAccount;
import com.ws.bitesmart.entity.merchant.MerchantFundLedger;
import com.ws.bitesmart.entity.merchant.MerchantSettlement;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.order.PaymentLog;
import com.ws.bitesmart.entity.refund.RefundApplication;
import com.ws.bitesmart.mapper.merchant.MerchantFundAccountMapper;
import com.ws.bitesmart.mapper.merchant.MerchantFundLedgerMapper;
import com.ws.bitesmart.mapper.merchant.MerchantSettlementMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantFinanceServiceTest {

    @Mock private MerchantFundAccountMapper accountMapper;
    @Mock private MerchantFundLedgerMapper ledgerMapper;
    @Mock private MerchantSettlementMapper settlementMapper;
    @Mock private OrdersMapper ordersMapper;

    @Test
    void paymentCreditsNetMerchantIncomeAndRecordsCommission() {
        MerchantFundAccount account = account();
        Orders order = new Orders();
        order.setId(11L);
        order.setMerchantId(20001L);
        order.setOrderNo("ORD-1");
        order.setPayAmount(new BigDecimal("10.00"));
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setId(22L);
        when(accountMapper.findByMerchantIdForUpdate(20001L)).thenReturn(account);
        when(ledgerMapper.findByIdempotencyKey("PAYMENT:ORD-1:INCOME")).thenReturn(null);
        when(accountMapper.updateBalances(account)).thenReturn(1);

        service().recordPaymentIncome(order, paymentLog);

        assertThat(account.getPendingBalance()).isEqualByComparingTo("9.00");
        assertThat(account.getTotalIncome()).isEqualByComparingTo("10.00");
        assertThat(account.getTotalCommission()).isEqualByComparingTo("1.00");
        ArgumentCaptor<MerchantFundLedger> captor = ArgumentCaptor.forClass(MerchantFundLedger.class);
        verify(ledgerMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(MerchantFundLedger::getAmount)
                .containsExactly(new BigDecimal("10.00"), new BigDecimal("1.00"));
    }

    @Test
    void repeatedPaymentCallbackDoesNotCreateAnotherLedger() {
        Orders order = new Orders();
        order.setMerchantId(20001L);
        order.setOrderNo("ORD-1");
        when(ledgerMapper.findByIdempotencyKey("PAYMENT:ORD-1:INCOME"))
                .thenReturn(new MerchantFundLedger());

        service().recordPaymentIncome(order, new PaymentLog());

        verify(accountMapper, never()).findByMerchantIdForUpdate(20001L);
        verify(ledgerMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refundBeforeCompletionReversesPendingNetIncome() {
        MerchantFundAccount account = account();
        account.setPendingBalance(new BigDecimal("9.00"));
        account.setTotalIncome(new BigDecimal("10.00"));
        account.setTotalCommission(new BigDecimal("1.00"));
        Orders order = new Orders();
        order.setId(11L);
        order.setMerchantId(20001L);
        order.setOrderNo("ORD-1");
        order.setOrderStatus(20);
        RefundApplication refund = new RefundApplication();
        refund.setId(33L);
        refund.setRefundAmount(new BigDecimal("10.00"));
        when(ledgerMapper.findByIdempotencyKey("REFUND:33:MERCHANT")).thenReturn(null);
        when(accountMapper.findByMerchantIdForUpdate(20001L)).thenReturn(account);
        when(accountMapper.updateBalances(account)).thenReturn(1);

        service().recordRefund(order, refund, null);

        assertThat(account.getPendingBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getTotalRefund()).isEqualByComparingTo("10.00");
        assertThat(account.getTotalCommission()).isEqualByComparingTo("0.00");
        verify(ledgerMapper).insert(org.mockito.ArgumentMatchers.any(MerchantFundLedger.class));
    }

    @Test
    void creatingSettlementFreezesAvailableBalance() {
        MerchantFundAccount account = account();
        account.setAvailableBalance(new BigDecimal("100.00"));
        when(accountMapper.findByMerchantIdForUpdate(20001L)).thenReturn(account);
        when(settlementMapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(ledgerMapper.findByIdempotencyKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(accountMapper.updateBalances(account)).thenReturn(1);

        MerchantSettlement settlement = service().createSettlement(20001L, new BigDecimal("30.00"),
                "ALIPAY", "****1234", 9L);

        assertThat(settlement.getSettlementStatus()).isEqualTo(10);
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("70.00");
        assertThat(account.getFrozenBalance()).isEqualByComparingTo("30.00");
    }

    @Test
    void completingSettlementReleasesFrozenBalance() {
        MerchantFundAccount account = account();
        account.setFrozenBalance(new BigDecimal("30.00"));
        MerchantSettlement settlement = settlement(50L, 20, new BigDecimal("30.00"));
        when(settlementMapper.findById(50L)).thenReturn(settlement);
        when(accountMapper.findByMerchantIdForUpdate(20001L)).thenReturn(account);
        when(ledgerMapper.findByIdempotencyKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(settlementMapper.transitionStatus(eq(50L), eq(20), eq(30), eq(9L), eq("paid"), any())).thenReturn(1);
        when(accountMapper.updateBalances(account)).thenReturn(1);

        service().completeSettlement(50L, 9L, "paid");

        assertThat(account.getFrozenBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectingSettlementReturnsFrozenBalance() {
        MerchantFundAccount account = account();
        account.setAvailableBalance(new BigDecimal("70.00"));
        account.setFrozenBalance(new BigDecimal("30.00"));
        MerchantSettlement settlement = settlement(51L, 10, new BigDecimal("30.00"));
        when(settlementMapper.findById(51L)).thenReturn(settlement);
        when(accountMapper.findByMerchantIdForUpdate(20001L)).thenReturn(account);
        when(ledgerMapper.findByIdempotencyKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(settlementMapper.transitionStatus(eq(51L), eq(10), eq(40), eq(9L), eq("reject"), any())).thenReturn(1);
        when(accountMapper.updateBalances(account)).thenReturn(1);

        service().rejectSettlement(51L, 9L, "reject");

        assertThat(account.getAvailableBalance()).isEqualByComparingTo("100.00");
        assertThat(account.getFrozenBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void firstFinanceReadBackfillsHistoricalPaidOrdersOnce() {
        MerchantFundAccount account = account();
        Orders order = new Orders();
        order.setId(91L);
        order.setMerchantId(20001L);
        order.setOrderNo("OLD-1");
        order.setPayAmount(new BigDecimal("10.00"));
        order.setOrderStatus(50);
        when(accountMapper.findByMerchantId(20001L)).thenReturn(account);
        when(ordersMapper.findPaidByMerchantId(20001L)).thenReturn(java.util.List.of(order));
        when(accountMapper.findByMerchantIdForUpdate(20001L)).thenReturn(account);
        when(ledgerMapper.findByIdempotencyKey("PAYMENT:OLD-1:INCOME")).thenReturn(null);
        when(ledgerMapper.findByIdempotencyKey("ORDER:OLD-1:RELEASE:PENDING")).thenReturn(null);
        when(accountMapper.updateBalances(account)).thenReturn(1);

        MerchantFundAccount result = service().getOrCreateAccount(20001L);

        assertThat(result.getAvailableBalance()).isEqualByComparingTo("9.00");
        assertThat(result.getTotalIncome()).isEqualByComparingTo("10.00");
        assertThat(result.getTotalCommission()).isEqualByComparingTo("1.00");
    }

    private MerchantSettlement settlement(Long id, int status, BigDecimal amount) {
        MerchantSettlement settlement = new MerchantSettlement();
        settlement.setId(id);
        settlement.setMerchantId(20001L);
        settlement.setSettlementStatus(status);
        settlement.setNetAmount(amount);
        return settlement;
    }

    private MerchantFundAccount account() {
        MerchantFundAccount account = new MerchantFundAccount();
        account.setId(1L);
        account.setMerchantId(20001L);
        account.setPendingBalance(BigDecimal.ZERO);
        account.setAvailableBalance(BigDecimal.ZERO);
        account.setFrozenBalance(BigDecimal.ZERO);
        account.setTotalIncome(BigDecimal.ZERO);
        account.setTotalRefund(BigDecimal.ZERO);
        account.setTotalCommission(BigDecimal.ZERO);
        account.setVersion(0L);
        return account;
    }

    private MerchantFinanceService service() {
        return new MerchantFinanceService(accountMapper, ledgerMapper, settlementMapper, ordersMapper);
    }
}
