"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const feedback_1 = require("../../api/feedback");
const user_route_1 = require("../../utils/user-route");
Page({ data: { orderId: 1 }, onLoad(options) { (0, user_route_1.requireUser)(); if (options?.orderId)
        this.setData({ orderId: Number(options.orderId) }); }, back() { wx.navigateBack(); }, submit() { (0, feedback_1.createComplaint)({ orderId: this.data.orderId, targetType: 10, targetId: this.data.orderId, complaintReason: '订单服务反馈', complaintDesc: '用户提交的订单问题反馈' }).then(() => wx.showToast({ title: '反馈已提交', icon: 'none' })).catch((error) => wx.showToast({ title: error.message || '提交失败', icon: 'none' })); } });
