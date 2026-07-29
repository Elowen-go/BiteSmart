package com.ws.bitesmart.mapper.review;

import com.ws.bitesmart.entity.review.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.math.BigDecimal;

/**
 * 评价 Mapper
 *
 * 支持用户端和商家端的评价查询，以及商家回复、管理员审核等操作。
 */
@Mapper
public interface ReviewMapper {

    /** 根据 ID 查询评价 */
    Review findById(@Param("id") Long id);

    /** 根据订单 ID 查询评价 */
    Review findByOrderId(@Param("orderId") Long orderId);

    /** 查某商家的评价列表 */
    List<Review> findByMerchantId(@Param("merchantId") Long merchantId);

    @Select("SELECT COALESCE(AVG(overall_rating), 0) FROM review "
            + "WHERE merchant_id = #{merchantId} AND deleted = 0 AND overall_rating IS NOT NULL")
    BigDecimal averageOverallByMerchantId(@Param("merchantId") Long merchantId);

    /** 查某用户的评价列表 */
    List<Review> findByUserId(@Param("userId") Long userId);

    /** 查某配送员的评价列表 */
    List<Review> findByDriverId(@Param("driverId") Long driverId);

    /** 新增评价 */
    int insert(Review review);

    /** 商家回复评价 */
    int updateReply(@Param("id") Long id,
                    @Param("merchantReply") String merchantReply,
                    @Param("merchantReplyTime") java.time.LocalDateTime merchantReplyTime);

    /** 更新评价状态（管理员隐藏/删除） */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 分页查询所有评价 */
    List<Review> findAll();
    @Select("SELECT COUNT(*) FROM review WHERE merchant_id = #{merchantId} AND deleted = 0 AND create_time >= #{start} AND create_time <= #{end}")
    int countByMerchantAndTime(@Param("merchantId") Long merchantId,
                               @Param("start") java.time.LocalDateTime start,
                               @Param("end") java.time.LocalDateTime end);

}
