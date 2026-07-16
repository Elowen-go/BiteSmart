"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const feedback_1 = require("../../api/feedback");
const user_route_1 = require("../../utils/user-route");
Page({ data: { stars: [1, 2, 3, 4, 5], rating: 5, orderId: 1 }, onLoad(options) { (0, user_route_1.requireUser)(); if (options?.orderId)
        this.setData({ orderId: Number(options.orderId) }); }, back() { wx.navigateBack(); }, rate(event) { this.setData({ rating: Number(event.currentTarget.dataset.index) + 1 }); }, submit() { (0, feedback_1.createReview)(this.data.orderId, { ratingFood: this.data.rating, ratingDelivery: this.data.rating, ratingService: this.data.rating, overallRating: this.data.rating, content: '整体体验很好', isAnonymous: 0 }).then(() => wx.showToast({ title: '评价已提交', icon: 'none' })).catch((error) => wx.showToast({ title: error.message || '提交失败', icon: 'none' })); } });
