package com.ws.bitesmart.mapper.system;

import com.ws.bitesmart.entity.system.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公告 Mapper
 *
 * 支持公告的 CRUD 以及分页查询。
 */
@Mapper
public interface NoticeMapper {

    /** 查询所有公告（分页），按发布时间倒序 */
    List<Notice> findAll();

    /** 查询已发布的公告列表（用户端） */
    List<Notice> findPublished();

    /** 根据 ID 查询 */
    Notice findById(@Param("id") Long id);

    /** 新增公告 */
    int insert(Notice notice);

    /** 更新公告 */
    int updateById(Notice notice);

    /** 逻辑删除 */
    int deleteById(@Param("id") Long id);

}
