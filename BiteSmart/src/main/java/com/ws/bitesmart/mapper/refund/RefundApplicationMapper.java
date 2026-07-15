package com.ws.bitesmart.mapper.refund;

import com.ws.bitesmart.entity.refund.RefundApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RefundApplicationMapper {
    List<RefundApplication> findAll(@Param("auditStatus") Integer auditStatus);
    RefundApplication findById(@Param("id") Long id);
    long countByStatus(@Param("auditStatus") Integer auditStatus);
    int updateAudit(@Param("id") Long id, @Param("status") Integer status,
                    @Param("operatorId") Long operatorId, @Param("remark") String remark);
}
