"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const order_1 = require("../../api/order");
const user_route_1 = require("../../utils/user-route");
Page({
    data: { loading: false },
    onLoad() { (0, user_route_1.requireUser)(); },
    back() { wx.navigateBack(); },
    submit() {
        if (this.data.loading)
            return;
        this.setData({ loading: true });
        (0, order_1.createOrder)({ address: '滨江创意园 A 座 3 楼', receiverName: '安之', receiverPhone: '13800002026', remark: '' }).then(() => wx.redirectTo({ url: '/pages/pay-result/pay-result' })).catch((error) => wx.showToast({ title: error.message || '订单提交失败', icon: 'none' })).finally(() => this.setData({ loading: false }));
    }
});
