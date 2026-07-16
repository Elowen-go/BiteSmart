"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const order_1 = require("../../api/order");
const user_route_1 = require("../../utils/user-route");
Page({
    data: { orders: [], loading: false },
    onLoad() { if (!(0, user_route_1.requireUser)())
        return; this.setData({ loading: true }); (0, order_1.getOrders)().then((result) => this.setData({ orders: Array.isArray(result) ? result : result.records })).catch(() => { }).finally(() => this.setData({ loading: false })); },
    back() { wx.navigateBack(); },
    openDetail() { wx.navigateTo({ url: '/pages/order-detail/order-detail' }); }
});
