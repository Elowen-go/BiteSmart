package com.ws.bitesmart.service.review;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.delivery.DeliveryDriver;
import com.ws.bitesmart.entity.order.Orders;
import com.ws.bitesmart.entity.review.Review;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.delivery.DeliveryDriverMapper;
import com.ws.bitesmart.mapper.order.OrdersMapper;
import com.ws.bitesmart.mapper.review.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价服务
 *
 * 用户：创建评价、查询自己的评价
 * 商家：查询收到的评价、回复评价
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrdersMapper ordersMapper;
    private final DeliveryDriverMapper deliveryDriverMapper;

    /**
     * 用户创建评价
     *
     * @param userId   用户ID
     * @param orderId  订单ID
     * @param review   评价内容（含各项评分）
     * @return 创建后的评价ID
     */
    @Transactional
    public Long createReview(Long userId, Long orderId, Review review) {
        // 1. 校验订单存在且属于该用户
        Orders order = ordersMapper.findById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权评价此订单");
        }

        // 2. 校验订单状态（已完成才能评价）
        if (order.getOrderStatus() != 50) {
            throw new BusinessException(ResultCodeEnum.ORDER_STATUS_ERROR, "仅已完成订单可评价");
        }

        // 3. 校验是否已评价
        Review existing = reviewMapper.findByOrderId(orderId);
        if (existing != null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "该订单已评价");
        }

        // 4. 计算综合评分（三项评分的平均值，四舍五入保留一位小数）
        BigDecimal food = BigDecimal.valueOf(review.getRatingFood() != null ? review.getRatingFood() : 0);
        BigDecimal delivery = BigDecimal.valueOf(review.getRatingDelivery() != null ? review.getRatingDelivery() : 0);
        BigDecimal service = BigDecimal.valueOf(review.getRatingService() != null ? review.getRatingService() : 0);
        BigDecimal overall = food.add(delivery).add(service)
                .divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);

        // 5. 填充数据并写入
        review.setId(SnowflakeUtil.generate());
        review.setUserId(userId);
        review.setMerchantId(order.getMerchantId());
        review.setOrderId(orderId);
        review.setOverallRating(overall);
        review.setStatus(10); // 已发布
        reviewMapper.insert(review);

        log.info("评价创建成功: reviewId={}, orderId={}, userId={}", review.getId(), orderId, userId);
        return review.getId();
    }

    /**
     * 用户查询自己的评价列表
     */
    public List<Review> getMyReviews(Long userId) {
        return reviewMapper.findByUserId(userId);
    }

    /**
     * 用户查询自己的评价列表（分页）
     */
    public PageInfo<Review> findByUserId(Long userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Review> list = reviewMapper.findByUserId(userId);
        return new PageInfo<>(list);
    }

    /**
     * 商家查询收到的评价列表
     */
    public List<Review> getMerchantReviews(Long merchantId) {
        return reviewMapper.findByMerchantId(merchantId);
    }

    /**
     * 商家查询收到的评价列表（分页）
     */
    public PageInfo<Review> findByMerchantId(Long merchantId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Review> list = reviewMapper.findByMerchantId(merchantId);
        return new PageInfo<>(list);
    }

    /**
     * 商家回复评价
     *
     * @param id             评价ID
     * @param merchantId     商家ID
     * @param merchantReply  回复内容
     */
    @Transactional
    public void replyReview(Long id, Long merchantId, String merchantReply) {
        Review review = reviewMapper.findById(id);
        if (review == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "评价不存在");
        }
        if (!review.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权回复此评价");
        }
        if (review.getMerchantReply() != null) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "该评价已回复");
        }

        reviewMapper.updateReply(id, merchantReply, LocalDateTime.now());
        log.info("商家回复评价成功: reviewId={}, merchantId={}", id, merchantId);
    }

    /**
     * 配送员查看收到的评价列表
     *
     * @param userId 配送员用户ID
     * @return 评价列表
     */
    public List<Review> findByDriverId(Long userId) {
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        return reviewMapper.findByDriverId(driver.getId());
    }

    /**
     * 配送员查看配送评分统计
     *
     * @param userId 配送员用户ID
     * @return 包含评分平均值、各分数段数量的统计Map
     */
    public Map<String, Object> getDriverRatingStats(Long userId) {
        DeliveryDriver driver = deliveryDriverMapper.findByUserId(userId);
        if (driver == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "配送员信息不存在");
        }
        List<Review> reviews = reviewMapper.findByDriverId(driver.getId());

        // 过滤出有配送评分的记录
        List<Review> withRating = reviews.stream()
                .filter(r -> r.getRatingDelivery() != null)
                .toList();

        // 计算平均配送评分
        double avg = withRating.stream()
                .mapToInt(Review::getRatingDelivery)
                .average()
                .orElse(0.0);
        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);

        // 统计各分数段数量（1-5分）
        long count1 = withRating.stream().filter(r -> r.getRatingDelivery() == 1).count();
        long count2 = withRating.stream().filter(r -> r.getRatingDelivery() == 2).count();
        long count3 = withRating.stream().filter(r -> r.getRatingDelivery() == 3).count();
        long count4 = withRating.stream().filter(r -> r.getRatingDelivery() == 4).count();
        long count5 = withRating.stream().filter(r -> r.getRatingDelivery() == 5).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", avgRating);
        stats.put("totalReviews", withRating.size());
        stats.put("rating1Count", count1);
        stats.put("rating2Count", count2);
        stats.put("rating3Count", count3);
        stats.put("rating4Count", count4);
        stats.put("rating5Count", count5);
        return stats;
    }
}
