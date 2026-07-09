package com.ws.bitesmart.controller.merchant;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.review.Review;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端 - 评价管理接口
 *
 * 商家查看收到的评价、回复评价。
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant/reviews")
@RequiredArgsConstructor
public class MerchantReviewController {

    private final ReviewService reviewService;

    /** 商家收到的评价列表 */
    @GetMapping
    public ResultVO<?> list(@AuthenticationPrincipal LoginUser loginUser,
                            @RequestParam(required = false) Integer page,
                            @RequestParam(defaultValue = "10") int size) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (page != null) {
            return ResultVO.success(PageResultVO.success(reviewService.findByMerchantId(loginUser.getUserId(), page, size)));
        }
        return ResultVO.success(reviewService.getMerchantReviews(loginUser.getUserId()));
    }

    /** 商家回复评价 */
    @PostMapping("/{id}/reply")
    public ResultVO<Void> reply(@AuthenticationPrincipal LoginUser loginUser,
                                 @PathVariable Long id,
                                 @RequestParam String content) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        reviewService.replyReview(id, loginUser.getUserId(), content);
        return ResultVO.ok("回复成功");
    }

}
