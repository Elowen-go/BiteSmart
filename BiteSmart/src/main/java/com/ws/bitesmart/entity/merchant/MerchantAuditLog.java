package com.ws.bitesmart.entity.merchant;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商家入驻审核日志表 实体类
 *
 * 对应 merchant_audit_log 表。
 * 商家每次提交入驻/修改资料都会生成一条审核记录。
 */
@Data
public class MerchantAuditLog {

    private Long id;
    private Long merchantId;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 提交的申请数据快照（JSON） */
    private String submitData;

    /** 审核人ID */
    private Long auditOperatorId;

    /** 审核人姓名 */
    private String auditOperatorName;

    /** 审核结果：10-待审核 20-通过 30-驳回 */
    private Integer auditStatus;

    /** 审核备注/驳回原因 */
    private String auditRemark;

    /** 审核时间 */
    private LocalDateTime auditTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
