package com.ws.bitesmart.mapper.file;

import com.ws.bitesmart.entity.file.FileUploadRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件上传记录 Mapper
 */
@Mapper
public interface FileUploadRecordMapper {

    FileUploadRecord findById(@Param("id") Long id);

    List<FileUploadRecord> findByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    int insert(FileUploadRecord record);

    int deleteById(@Param("id") Long id);

}
