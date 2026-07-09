package com.ws.bitesmart.controller.review;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.review.Review;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 - 评价管理接口
 *
 * 用户提交评价、查看自己的评价列表。
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class UserReviewController {

    private final ReviewService reviewService;

    /** 用户提交评价 */
    @PostMapping
    public ResultVO<Long> create(@AuthenticationPrincipal LoginUser loginUser,
                                  @RequestParam Long orderId,
                                  @RequestBody Review review) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        Long reviewId = reviewService.createReview(loginUser.getUserId(), orderId, review);
        return ResultVO.success("评价成功", reviewId);
    }

    /** 用户的评价列表 */
    @GetMapping("/my")
    public ResultVO<?> myReviews(@AuthenticationPrincipal LoginUser loginUser,
                                  @RequestParam(required = false) Integer page,
                                  @RequestParam(defaultValue = "10") int size) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (page != null) {
            return ResultVO.success(PageResultVO.success(reviewService.findByUserId(loginUser.getUserId(), page, size)));
        }
        return ResultVO.success(reviewService.getMyReviews(loginUser.getUserId()));
    }

}
