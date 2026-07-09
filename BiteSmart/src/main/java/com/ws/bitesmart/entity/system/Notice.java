package com.ws.bitesmart.entity.system;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公告管理表 实体类
 *
 * 对应 notice 表。
 * 管理员发布系统公告/新闻/活动推广，用户端可查看已发布的公告。
 */
@Data
public class Notice {

    /** 主键ID */
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 类型：notice-公告 news-新闻 promotion-推广 */
    private String type;

    /** 状态：10-发布 20-草稿 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
