package com.ws.bitesmart.mapper.complaint;

import com.ws.bitesmart.entity.complaint.ComplaintTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ComplaintTicketMapper {
    int insert(ComplaintTicket ticket);
    List<ComplaintTicket> findAll(@Param("status") Integer status);
    ComplaintTicket findById(@Param("id") Long id);
    int updateHandle(@Param("id") Long id, @Param("status") Integer status,
                     @Param("operatorId") Long operatorId, @Param("remark") String remark,
                     @Param("result") String result);
}
