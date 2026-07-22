package com.ws.bitesmart.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 骑手轨迹点上传请求
 *
 * 坐标系约定：GCJ-02（高德/腾讯地图坐标系）。
 * 小程序端请用 wx.getLocation({ type: 'gcj02' }) 获取，或自行把 wgs84 转换为 GCJ-02 后再上传。
 */
@Data
public class RiderLocationRequest {

    /** 配送任务ID */
    private Long taskId;

    /** 纬度（GCJ-02） */
    private BigDecimal latitude;

    /** 经度（GCJ-02） */
    private BigDecimal longitude;

}
