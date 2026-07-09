package com.ws.bitesmart.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件上传记录表 实体类
 *
 * 对应 file_upload_record 表。
 * 存的是相对路径，不是绝对路径。
 * file_url 字段格式：/uploads/2026/07/09/xxx.jpg
 */
@Data
public class FileUploadRecord {

    /** 主键ID */
    private Long id;

    /** 上传用户ID */
    private Long userId;

    /** 业务类型：avatar/dish_image/license/review_image */
    private String bizType;

    /** 关联业务ID */
    private Long bizId;

    /** 原始文件名 */
    private String fileName;

    /** 文件相对路径（存到数据库的路径，不是磁盘绝对路径） */
    private String fileUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件MIME类型 */
    private String mimeType;

    /** 存储服务内部路径 */
    private String storagePath;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
