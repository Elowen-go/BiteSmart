package com.ws.bitesmart.service.merchant;

import com.ws.bitesmart.entity.merchant.Merchant;
import com.ws.bitesmart.mapper.merchant.MerchantAuditLogMapper;
import com.ws.bitesmart.mapper.merchant.MerchantMapper;
import com.ws.bitesmart.mapper.user.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMerchantServiceTest {

    @Mock private MerchantMapper merchantMapper;
    @Mock private MerchantAuditLogMapper auditLogMapper;
    @Mock private SysUserMapper sysUserMapper;

    @Test
    void approvingMerchantSynchronizesUserRole() {
        Merchant merchant = new Merchant();
        merchant.setId(200L);
        merchant.setUserId(100L);
        merchant.setStatus(10);
        when(merchantMapper.findById(200L)).thenReturn(merchant);
        when(merchantMapper.updateStatus(200L, 20, "资料齐全")).thenReturn(1);

        service().audit(200L, 20, "资料齐全", 900L);

        verify(sysUserMapper).updateRoleType(100L, 20);
    }

    private AdminMerchantService service() {
        return new AdminMerchantService(merchantMapper, auditLogMapper, sysUserMapper);
    }
}
